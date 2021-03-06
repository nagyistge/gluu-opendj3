/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2008-2010 Sun Microsystems, Inc.
 * Portions Copyright 2013-2016 ForgeRock AS.
 */

package org.opends.guitools.controlpanel.task;

import static org.opends.messages.AdminToolMessages.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.opends.admin.ads.util.ConnectionUtils;
import org.opends.guitools.controlpanel.browser.BrowserController;
import org.opends.guitools.controlpanel.datamodel.BackendDescriptor;
import org.opends.guitools.controlpanel.datamodel.BaseDNDescriptor;
import org.opends.guitools.controlpanel.datamodel.ControlPanelInfo;
import org.opends.guitools.controlpanel.datamodel.CustomSearchResult;
import org.opends.guitools.controlpanel.ui.ColorAndFontConstants;
import org.opends.guitools.controlpanel.ui.ProgressDialog;
import org.opends.guitools.controlpanel.ui.nodes.BasicNode;
import org.opends.guitools.controlpanel.ui.nodes.BrowserNodeInfo;
import org.opends.guitools.controlpanel.util.Utilities;
import org.forgerock.i18n.LocalizableMessage;
import org.opends.server.schema.SchemaConstants;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.util.ServerConstants;

/**
 * The task that is launched when an entry must be deleted.
 */
public class DeleteEntryTask extends Task
{
  private Set<String> backendSet;
  private DN lastDn;
  private int nDeleted;
  private int nToDelete = -1;
  private BrowserController controller;
  private TreePath[] paths;
  private long lastProgressTime;
  private boolean equivalentCommandWithControlPrinted;
  private boolean equivalentCommandWithoutControlPrinted;
  private boolean useAdminCtx;

  /**
   * Constructor of the task.
   * @param info the control panel information.
   * @param dlg the progress dialog where the task progress will be displayed.
   * @param paths the tree paths of the entries that must be deleted.
   * @param controller the Browser Controller.
   */
  public DeleteEntryTask(ControlPanelInfo info, ProgressDialog dlg,
      TreePath[] paths, BrowserController controller)
  {
    super(info, dlg);
    backendSet = new HashSet<>();
    this.controller = controller;
    this.paths = paths;
    SortedSet<DN> entries = new TreeSet<>();
    boolean canPrecalculateNumberOfEntries = true;
    nToDelete = paths.length;
    for (TreePath path : paths)
    {
      BasicNode node = (BasicNode)path.getLastPathComponent();
      entries.add(DN.valueOf(node.getDN()));
    }
    for (BackendDescriptor backend : info.getServerDescriptor().getBackends())
    {
      for (BaseDNDescriptor baseDN : backend.getBaseDns())
      {
        for (DN dn : entries)
        {
          if (dn.isSubordinateOrEqualTo(baseDN.getDn()))
          {
            backendSet.add(backend.getBackendID());
            break;
          }
        }
      }
    }
    if (!canPrecalculateNumberOfEntries)
    {
      nToDelete = -1;
    }
  }

  /** {@inheritDoc} */
  public Type getType()
  {
    return Type.DELETE_ENTRY;
  }

  /** {@inheritDoc} */
  public Set<String> getBackends()
  {
    return backendSet;
  }

  /** {@inheritDoc} */
  public LocalizableMessage getTaskDescription()
  {
    return INFO_CTRL_PANEL_DELETE_ENTRY_TASK_DESCRIPTION.get();
  }

  /** {@inheritDoc} */
  protected String getCommandLinePath()
  {
    return null;
  }

  /** {@inheritDoc} */
  protected ArrayList<String> getCommandLineArguments()
  {
    return new ArrayList<>();
  }

  /** {@inheritDoc} */
  public boolean canLaunch(Task taskToBeLaunched,
      Collection<LocalizableMessage> incompatibilityReasons)
  {
    if (!isServerRunning()
        && state == State.RUNNING
        && runningOnSameServer(taskToBeLaunched))
    {
      // All the operations are incompatible if they apply to this
      // backend for safety.
      Set<String> backends = new TreeSet<>(taskToBeLaunched.getBackends());
      backends.retainAll(getBackends());
      if (!backends.isEmpty())
      {
        incompatibilityReasons.add(getIncompatibilityMessage(this, taskToBeLaunched));
        return false;
      }
    }
    return true;
  }

  /** {@inheritDoc} */
  public boolean regenerateDescriptor()
  {
    return false;
  }

  /** {@inheritDoc} */
  public void runTask()
  {
    state = State.RUNNING;
    lastException = null;

    ArrayList<DN> alreadyDeleted = new ArrayList<>();
    ArrayList<BrowserNodeInfo> toNotify = new ArrayList<>();
    try
    {
      for (TreePath path : paths)
      {
        BasicNode node = (BasicNode)path.getLastPathComponent();
        try
        {
          DN dn = DN.valueOf(node.getDN());
          boolean isDnDeleted = false;
          for (DN deletedDn : alreadyDeleted)
          {
            if (dn.isSubordinateOrEqualTo(deletedDn))
            {
              isDnDeleted = true;
              break;
            }
          }
          if (!isDnDeleted)
          {
            InitialLdapContext ctx =
              controller.findConnectionForDisplayedEntry(node);
            useAdminCtx = controller.isConfigurationNode(node);
            if (node.hasSubOrdinates())
            {
              deleteSubtreeWithControl(ctx, dn, path, toNotify);
            }
            else
            {
              deleteSubtreeRecursively(ctx, dn, path, toNotify);
            }
            alreadyDeleted.add(dn);
          }
        }
        catch (DirectoryException de)
        {
          throw new RuntimeException("Unexpected error parsing dn: "+
              node.getDN(), de);
        }
      }
      if (!toNotify.isEmpty())
      {
        final List<BrowserNodeInfo> fToNotify = new ArrayList<>(toNotify);
        toNotify.clear();
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            notifyEntriesDeleted(fToNotify);
          }
        });
      }
      state = State.FINISHED_SUCCESSFULLY;
    }
    catch (Throwable t)
    {
      lastException = t;
      state = State.FINISHED_WITH_ERROR;
    }
    if (nDeleted > 1)
    {
      getProgressDialog().appendProgressHtml(Utilities.applyFont(
          "<br>"+INFO_CTRL_PANEL_ENTRIES_DELETED.get(nDeleted),
          ColorAndFontConstants.progressFont));
    }
  }

  /**
   * Notifies that some entries have been deleted.  This will basically update
   * the browser controller so that the tree reflects the changes that have
   * been made.
   * @param deletedNodes the nodes that have been deleted.
   */
  private void notifyEntriesDeleted(Collection<BrowserNodeInfo> deletedNodes)
  {
    TreePath pathToSelect = null;
    for (BrowserNodeInfo nodeInfo : deletedNodes)
    {
      TreePath parentPath = controller.notifyEntryDeleted(nodeInfo);
      if (pathToSelect != null)
      {
        if (parentPath.getPathCount() < pathToSelect.getPathCount())
        {
          pathToSelect = parentPath;
        }
      }
      else
      {
        pathToSelect = parentPath;
      }
    }
    if (pathToSelect != null)
    {
      TreePath selectedPath = controller.getTree().getSelectionPath();
      if (selectedPath == null)
      {
        controller.getTree().setSelectionPath(pathToSelect);
      }
      else if (!selectedPath.equals(pathToSelect) &&
          pathToSelect.getPathCount() < selectedPath.getPathCount())
      {
        controller.getTree().setSelectionPath(pathToSelect);
      }
    }
  }

  private void deleteSubtreeRecursively(InitialLdapContext ctx, DN dnToRemove,
      TreePath path, ArrayList<BrowserNodeInfo> toNotify)
  throws NamingException, DirectoryException
  {
    lastDn = dnToRemove;

    long t = System.currentTimeMillis();
    boolean canDelete = nToDelete > 0 && nToDelete > nDeleted;
    boolean displayProgress =
      canDelete && ((nDeleted % 20) == 0 || t - lastProgressTime > 5000);

    if (displayProgress)
    {
      // Only display the first entry equivalent command-line.
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          if (!equivalentCommandWithoutControlPrinted)
          {
            printEquivalentCommandToDelete(lastDn, false);
            equivalentCommandWithoutControlPrinted = true;
          }
          getProgressDialog().setSummary(
              LocalizableMessage.raw(
                  Utilities.applyFont(
                      INFO_CTRL_PANEL_DELETING_ENTRY_SUMMARY.get(lastDn),
                      ColorAndFontConstants.defaultFont)));
        }
      });
    }

    try
    {
      SearchControls ctls = new SearchControls();
      ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
      String filter =
        "(|(objectClass=*)(objectclass=ldapsubentry))";
      ctls.setReturningAttributes(
          new String[] { SchemaConstants.NO_ATTRIBUTES });
      NamingEnumeration<SearchResult> entryDNs =
        ctx.search(Utilities.getJNDIName(dnToRemove.toString()), filter, ctls);

      DN entryDNFound = dnToRemove;
      try
      {
        while (entryDNs.hasMore())
        {
          SearchResult sr = entryDNs.next();
          if (!sr.getName().equals(""))
          {
            CustomSearchResult res =
              new CustomSearchResult(sr, dnToRemove.toString());
            entryDNFound = DN.valueOf(res.getDN());
            deleteSubtreeRecursively(ctx, entryDNFound, null, toNotify);
          }
        }
      }
      finally
      {
        entryDNs.close();
      }

    } catch (NameNotFoundException nnfe) {
      // The entry is not there: it has been removed
    }

    try
    {
      ctx.destroySubcontext(Utilities.getJNDIName(dnToRemove.toString()));
      if (path != null)
      {
        toNotify.add(controller.getNodeInfoFromPath(path));
      }
      nDeleted ++;
      if (displayProgress)
      {
        lastProgressTime = t;
        final Collection<BrowserNodeInfo> fToNotify;
        if (!toNotify.isEmpty())
        {
          fToNotify = new ArrayList<>(toNotify);
          toNotify.clear();
        }
        else
        {
          fToNotify = null;
        }
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            getProgressDialog().getProgressBar().setIndeterminate(false);
            getProgressDialog().getProgressBar().setValue(
                (100 * nDeleted) / nToDelete);
            if (fToNotify != null)
            {
              notifyEntriesDeleted(fToNotify);
            }
          }
        });
      }
    } catch (NameNotFoundException nnfe)
    {
      // The entry is not there: it has been removed
    }
  }

  private void deleteSubtreeWithControl(InitialLdapContext ctx, DN dn,
      TreePath path, ArrayList<BrowserNodeInfo> toNotify)
  throws NamingException
  {
    lastDn = dn;
    long t = System.currentTimeMillis();
    //  Only display the first entry equivalent command-line.
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        if (!equivalentCommandWithControlPrinted)
        {
          printEquivalentCommandToDelete(lastDn, true);
          equivalentCommandWithControlPrinted = true;
        }
        getProgressDialog().setSummary(
            LocalizableMessage.raw(
                Utilities.applyFont(
                    INFO_CTRL_PANEL_DELETING_ENTRY_SUMMARY.get(lastDn),
                    ColorAndFontConstants.defaultFont)));
      }
    });
    //  Use a copy of the dir context since we are using an specific
    // control to delete the subtree and this can cause
    // synchronization problems when the tree is refreshed.
    InitialLdapContext ctx1 = null;
    try
    {
      ctx1 = ConnectionUtils.cloneInitialLdapContext(ctx,
          getInfo().getConnectTimeout(),
          getInfo().getTrustManager(), null);
      Control[] ctls = {
          new BasicControl(ServerConstants.OID_SUBTREE_DELETE_CONTROL)};
      ctx1.setRequestControls(ctls);
      ctx1.destroySubcontext(Utilities.getJNDIName(dn.toString()));
    }
    finally
    {
      try
      {
        ctx1.close();
      }
      catch (Throwable th)
      {
      }
    }
    nDeleted ++;
    lastProgressTime = t;
    if (path != null)
    {
      toNotify.add(controller.getNodeInfoFromPath(path));
    }
    final Collection<BrowserNodeInfo> fToNotify;
    if (!toNotify.isEmpty())
    {
      fToNotify = new ArrayList<>(toNotify);
      toNotify.clear();
    }
    else
    {
      fToNotify = null;
    }
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        getProgressDialog().getProgressBar().setIndeterminate(false);
        getProgressDialog().getProgressBar().setValue(
            (100 * nDeleted) / nToDelete);
        if (fToNotify != null)
        {
          notifyEntriesDeleted(fToNotify);
        }
      }
    });
  }

  /**
   * Prints in the progress dialog the equivalent command-line to delete a
   * subtree.
   * @param dn the DN of the subtree to be deleted.
   * @param usingControl whether we must include the control or not.
   */
  private void printEquivalentCommandToDelete(DN dn, boolean usingControl)
  {
    ArrayList<String> args = new ArrayList<>(getObfuscatedCommandLineArguments(
        getConnectionCommandLineArguments(useAdminCtx, true)));
    args.add(getNoPropertiesFileArgument());
    if (usingControl)
    {
      args.add("-J");
      args.add(ServerConstants.OID_SUBTREE_DELETE_CONTROL);
    }
    args.add(dn.toString());
    printEquivalentCommandLine(getCommandLinePath("ldapdelete"),
        args,
        INFO_CTRL_PANEL_EQUIVALENT_CMD_TO_DELETE_ENTRY.get(dn));
  }
}
