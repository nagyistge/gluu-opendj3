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
 * Copyright 2007-2008 Sun Microsystems, Inc.
 * Portions Copyright 2014-2016 ForgeRock AS.
 */
package org.opends.server.admin.client.ldap;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;
import org.opends.server.TestCaseUtils;
import org.opends.server.types.Entry;
import org.testng.Assert;


/**
 * A mock LDAP connection which fakes up search results based on some
 * LDIF content. Implementations should override the modify operations
 * in order to get provide the correct fake behavior.
 */
public class MockLDAPConnection extends LDAPConnection {

  /**
   * A mock entry.
   */
  private static final class MockEntry {

    /** The entry's attributes. */
    private final Attributes attributes;

    /** The entry's children. */
    private final List<MockEntry> children;

    /** The name of this mock entry. */
    private final DN dn;



    /**
     * Create a new mock entry with the provided name and attributes.
     *
     * @param dn
     *          The name of the entry.
     * @param attributes
     *          The attributes.
     */
    public MockEntry(DN dn, Attributes attributes) {
      this.dn = dn;
      this.attributes = attributes;
      this.children = new LinkedList<>();
    }



    /**
     * Get the entry's attributes.
     *
     * @return Returns the entry's attributes.
     */
    public Attributes getAttributes() {
      return attributes;
    }



    /**
     * Get the entry's children.
     *
     * @return Returns the entry's children.
     */
    public List<MockEntry> getChildren() {
      return children;
    }



    /**
     * Get the entry's name.
     *
     * @return Returns the entry's name.
     */
    public DN getDN() {
      return dn;
    }



    /** {@inheritDoc} */
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("dn=");
      builder.append(dn);
      builder.append(" attributes=");
      builder.append(attributes);
      return builder.toString();
    }
  }

  /** All the entries. */
  private final Map<DN, MockEntry> entries;

  /** The single root entry. */
  private final MockEntry rootEntry;



  /**
   * Creates a new mock LDAP connection.
   */
  public MockLDAPConnection() {
    this.rootEntry = new MockEntry(DN.rootDN(), new BasicAttributes());
    this.entries = new HashMap<>();
    this.entries.put(DN.rootDN(), this.rootEntry);
  }



  /** {@inheritDoc} */
  @Override
  public void createEntry(LdapName dn, Attributes attributes)
      throws NamingException {
    throw new UnsupportedOperationException("createEntry");
  }



  /** {@inheritDoc} */
  @Override
  public void deleteSubtree(LdapName dn) throws NamingException {
    throw new UnsupportedOperationException("deleteSubtree");
  }



  /** {@inheritDoc} */
  @Override
  public boolean entryExists(LdapName dn) throws NamingException {
    return getEntry(dn) != null;
  }



  /**
   * Imports the provided LDIF into this mock connection.
   *
   * @param lines
   *          The LDIF.
   */
  public final void importLDIF(String... lines) {
    try {
      for (Entry entry : TestCaseUtils.makeEntries(lines)) {
        addEntry(entry);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }



  /** {@inheritDoc} */
  @Override
  public Collection<LdapName> listEntries(LdapName dn, String filter) throws NamingException {
    MockEntry entry = getEntry(dn);
    if (entry == null) {
      throw new NameNotFoundException("could not find entry " + dn);
    }

    LinkedList<LdapName> names = new LinkedList<>();
    for (MockEntry child : entry.children)
    {
      names.add(new LdapName(child.getDN().toString()));
    }
    return names;
  }



  /** {@inheritDoc} */
  @Override
  public void modifyEntry(LdapName dn, Attributes mods) throws NamingException {
    throw new UnsupportedOperationException("modifyEntry");
  }



  /** {@inheritDoc} */
  @Override
  public Attributes readEntry(LdapName dn, Collection<String> attrIds)
      throws NamingException {
    MockEntry entry = getEntry(dn);

    if (entry == null) {
      throw new NameNotFoundException("could not find entry " + dn);
    } else if (attrIds.isEmpty()) {
      return entry.getAttributes();
    } else {
      Attributes attributes = entry.getAttributes();
      Attributes filteredAttributes = new BasicAttributes();
      for (String attrId : attrIds) {
        if (attributes.get(attrId) != null) {
          filteredAttributes.put(attributes.get(attrId));
        }
      }
      return filteredAttributes;
    }
  }



  /**
   * Asserts whether the provided attribute contains exactly the set
   * of values contained in the provided collection.
   *
   * @param attr
   *          The attribute.
   * @param values
   *          The expected values.
   * @throws NamingException
   *           If an unexpected problem occurred.
   */
  protected final void assertAttributeEquals(Attribute attr,
      Collection<String> values) throws NamingException {
    LinkedList<String> actualValues = new LinkedList<>();
    NamingEnumeration<?> ne = attr.getAll();
    while (ne.hasMore()) {
      actualValues.add(ne.next().toString());
    }

    if (actualValues.size() != values.size()
        || !actualValues.containsAll(values)) {
      Assert.fail("Attribute " + attr.getID() + " contains " + actualValues
          + " but expected " + values);
    }
  }



  /**
   * Create a new mock entry.
   *
   * @param entry
   *          The entry to be added.
   */
  private void addEntry(Entry entry) {
    MockEntry parent = rootEntry;
    DN entryDN = entry.getName();

    // Create required glue entries.
    for (int i = 0; i < entryDN.size() - 1; i++) {
      RDN rdn = entryDN.rdn(entryDN.size() - i - 1);
      DN dn = parent.getDN().child(rdn);

      if (!entries.containsKey(dn)) {
        MockEntry glue = new MockEntry(dn, new BasicAttributes());
        parent.getChildren().add(glue);
        entries.put(dn, glue);
      }

      parent = entries.get(dn);
    }

    // We now have the parent entry - so construct the new entry.
    Attributes attributes = new BasicAttributes();
    for (org.opends.server.types.Attribute attribute : entry.getAttributes()) {
      BasicAttribute ba = new BasicAttribute(attribute.getName());
      for (ByteString value : attribute) {
        ba.add(value.toString());
      }
      attributes.put(ba);
    }

    // Add object classes.
    BasicAttribute oc = new BasicAttribute("objectclass");
    for (String s : entry.getObjectClasses().values()) {
      oc.add(s);
    }
    attributes.put(oc);

    MockEntry child = new MockEntry(entryDN, attributes);
    parent.getChildren().add(child);
    entries.put(entryDN, child);
  }



  /**
   * Gets the named entry.
   *
   * @param dn
   *          The name of the entry.
   * @return Returns the mock entry or <code>null</code> if it does
   *         not exist.
   */
  private MockEntry getEntry(LdapName dn) {
    return entries.get(DN.valueOf(dn.toString()));
  }



  /** {@inheritDoc} */
  @Override
  public void unbind() {
    // nothing to do
  }

}
