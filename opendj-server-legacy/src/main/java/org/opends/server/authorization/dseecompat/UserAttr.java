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
 * Copyright 2008 Sun Microsystems, Inc.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */
package org.opends.server.authorization.dseecompat;

import java.util.LinkedList;
import java.util.List;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.SearchScope;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.internal.SearchRequest;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.opends.server.types.*;

import static org.opends.messages.AccessControlMessages.*;
import static org.opends.server.protocols.internal.InternalClientConnection.*;
import static org.opends.server.protocols.internal.Requests.*;

/*
 * TODO Evaluate making this class more efficient.
 *
 * This class isn't as efficient as it could be.  For example, the evalVAL()
 * method should be able to use cached versions of the attribute type and
 * filter. The evalURL() and evalDN() methods should also be able to use a
 * cached version of the attribute type.
 */
/**
 * This class implements the  userattr bind rule keyword.
 */
public class UserAttr implements KeywordBindRule {

    /**
     * This enumeration is the various types the userattr can have after
     * the "#" token.
     */
    private enum UserAttrType {
        USERDN, GROUPDN, ROLEDN, URL, VALUE;

        private static UserAttrType getType(String expr) throws AciException {
            if("userdn".equalsIgnoreCase(expr)) {
                return UserAttrType.USERDN;
            } else if("groupdn".equalsIgnoreCase(expr)) {
                 return UserAttrType.GROUPDN;
            } else if("roledn".equalsIgnoreCase(expr)) {
                return UserAttrType.ROLEDN;
            } else if("ldapurl".equalsIgnoreCase(expr)) {
                return UserAttrType.URL;
            }
            return UserAttrType.VALUE;
        }
    }

    /**
     * Used to create an attribute type that can compare the value below in
     * an entry returned from an internal search.
     */
    private String attrStr;

    /**
     * Used to compare a attribute value returned from a search against this
     * value which might have been defined in the ACI userattr rule.
     */
    private String attrVal;

    /** Contains the type of the userattr, one of the above enumerations. */
    private UserAttrType userAttrType;

    /** An enumeration representing the bind rule type. */
    private EnumBindRuleType type;

    /** The class used to hold the parent inheritance information. */
    private ParentInheritance parentInheritance;

    /**
     * Create an non-USERDN/GROUPDN instance of the userattr keyword class.
     * @param attrStr The attribute name in string form. Kept in string form
     * until processing.
     * @param attrVal The attribute value in string form -- used in the USERDN
     * evaluation for the parent hierarchy expression.
     * @param userAttrType The userattr type of the rule
     * "USERDN, GROUPDN, ...".
     * @param type The bind rule type "=, !=".
     */
    private UserAttr(String attrStr, String attrVal, UserAttrType userAttrType,
            EnumBindRuleType type) {
        this.attrStr=attrStr;
        this.attrVal=attrVal;
        this.userAttrType=userAttrType;
        this.type=type;
    }

    /**
     * Create an USERDN or GROUPDN  instance of the userattr keyword class.
     * @param userAttrType The userattr type of the rule (USERDN or GROUPDN)
     * only.
     * @param type The bind rule type "=, !=".
     * @param parentInheritance The parent inheritance class to use for parent
     * inheritance checks if any.
     */
    private UserAttr(UserAttrType userAttrType, EnumBindRuleType type,
                     ParentInheritance parentInheritance) {
        this.userAttrType=userAttrType;
        this.type=type;
        this.parentInheritance=parentInheritance;
    }
    /**
     * Decode an string containing the userattr bind rule expression.
     * @param expression The expression string.
     * @param type The bind rule type.
     * @return A class suitable for evaluating a userattr bind rule.
     * @throws AciException If the string contains an invalid expression.
     */
    public static KeywordBindRule decode(String expression,
                                         EnumBindRuleType type)
    throws AciException {
        String[] vals=expression.split("#");
        if(vals.length != 2) {
            LocalizableMessage message =
                WARN_ACI_SYNTAX_INVALID_USERATTR_EXPRESSION.get(expression);
            throw new AciException(message);
        }
        UserAttrType userAttrType = UserAttrType.getType(vals[1]);
        switch (userAttrType) {
                case GROUPDN:
                case USERDN: {
                    ParentInheritance parentInheritance =
                            new ParentInheritance(vals[0], false);
                    return new UserAttr (userAttrType, type, parentInheritance);
                }
                case ROLEDN: {
                  //The roledn keyword is not supported. Throw an exception with
                  //a message if it is seen in the expression.
                  throw new AciException(WARN_ACI_SYNTAX_ROLEDN_NOT_SUPPORTED.get(expression));
                }
         }
         return new UserAttr(vals[0], vals[1], userAttrType, type);
    }

    /**
     * Evaluate the expression using an evaluation context.
     * @param evalCtx   The evaluation context to use in the evaluation of the
     * userattr expression.
     * @return  An enumeration containing the result of the evaluation.
     */
    @Override
    public EnumEvalResult evaluate(AciEvalContext evalCtx) {
      EnumEvalResult matched;
      //The working resource entry might be filtered and not have an
      //attribute type that is needed to perform these evaluations. The
      //evalCtx has a copy of the non-filtered entry, switch to it for these
      //evaluations.
      switch(userAttrType) {
      case ROLEDN:
      case GROUPDN:
      case USERDN: {
        matched=evalDNKeywords(evalCtx);
        break;
      }
      case URL: {
        matched=evalURL(evalCtx);
        break;
      }
      default:
        matched=evalVAL(evalCtx);
      }
      return matched;
    }

    /** Evaluate a VALUE userattr type. Look in client entry for an
     *  attribute value and in the resource entry for the same
     *  value. If both entries have the same value than return true.
     * @param evalCtx The evaluation context to use.
     * @return An enumeration containing the result of the
     * evaluation.
     */
    private EnumEvalResult evalVAL(AciEvalContext evalCtx) {
        EnumEvalResult matched= EnumEvalResult.FALSE;
        boolean undefined=false;
        AttributeType attrType = DirectoryServer.getAttributeType(attrStr);
        final SearchRequest request = newSearchRequest(evalCtx.getClientDN(), SearchScope.BASE_OBJECT);
        InternalSearchOperation op = getRootConnection().processSearch(request);
        LinkedList<SearchResultEntry> result = op.getSearchEntries();
        if (!result.isEmpty()) {
            ByteString val= ByteString.valueOfUtf8(attrVal);
            SearchResultEntry resultEntry = result.getFirst();
            if(resultEntry.hasValue(attrType, val)) {
                Entry e=evalCtx.getResourceEntry();
                if(e.hasValue(attrType, val))
                {
                    matched=EnumEvalResult.TRUE;
                }
            }
        }
        return matched.getRet(type, undefined);
    }

    /**
     * Evaluate an URL userattr type. Look into the resource entry for the
     * specified attribute and values. Assume it is an URL. Decode it an try
     * and match it against the client entry attribute.
     * @param evalCtx  The evaluation context to evaluate with.
     * @return An enumeration containing a result of the URL evaluation.
     */
    private EnumEvalResult evalURL(AciEvalContext evalCtx) {
        EnumEvalResult matched= EnumEvalResult.FALSE;
        AttributeType attrType = DirectoryServer.getAttributeType(attrStr);
        List<Attribute> attrs=evalCtx.getResourceEntry().getAttribute(attrType);
        for(Attribute a : attrs) {
            for(ByteString v : a) {
                LDAPURL url;
                try {
                   url = LDAPURL.decode(v.toString(), true);
                } catch (DirectoryException e) {
                    break;
                }
                matched=UserDN.evalURL(evalCtx, url);
                if(matched != EnumEvalResult.FALSE)
                {
                    break;
                }
            }
            if (matched == EnumEvalResult.TRUE)
            {
                break;
            }
        }
        return matched.getRet(type, matched == EnumEvalResult.ERR);
    }

    /**
     * Evaluate the DN type userattr keywords. These are roledn, userdn and
     * groupdn. The processing is the same for all three, although roledn is
     * a slightly different. For the roledn userattr keyword, a very simple
     * parent inheritance class was created. The rest of the processing is the
     * same for all three keywords.
     *
     * @param evalCtx The evaluation context to evaluate with.
     * @return An enumeration containing a result of the USERDN evaluation.
     */
    private EnumEvalResult evalDNKeywords(AciEvalContext evalCtx) {
        EnumEvalResult matched= EnumEvalResult.FALSE;
        boolean undefined=false, stop=false;
        int numLevels=parentInheritance.getNumLevels();
        int[] levels=parentInheritance.getLevels();
        AttributeType attrType=parentInheritance.getAttributeType();
        DN baseDN=parentInheritance.getBaseDN();
        if(baseDN != null) {
            if (evalCtx.getResourceEntry().hasAttribute(attrType)) {
                matched=GroupDN.evaluate(evalCtx.getResourceEntry(),
                        evalCtx,attrType, baseDN);
            }
        } else {
        for(int i=0;(i < numLevels && !stop); i++ ) {
            //The ROLEDN keyword will always enter this statement. The others
            //might. For the add operation, the resource itself (level 0)
            //must never be allowed to give access.
            if(levels[i] == 0) {
                if(evalCtx.isAddOperation()) {
                    undefined=true;
                } else if (evalCtx.getResourceEntry().hasAttribute(attrType)) {
                    matched =
                            evalEntryAttr(evalCtx.getResourceEntry(),
                                    evalCtx,attrType);
                    if(matched.equals(EnumEvalResult.TRUE)) {
                        stop=true;
                    }
                }
            } else {
                DN pDN = getDNParentLevel(levels[i], evalCtx.getResourceDN());
                if(pDN == null) {
                    continue;
                }
                final SearchRequest request = newSearchRequest(pDN, SearchScope.BASE_OBJECT)
                    .addAttribute(parentInheritance.getAttrTypeStr());
                InternalSearchOperation op = getRootConnection().processSearch(request);
                LinkedList<SearchResultEntry> result = op.getSearchEntries();
                if (!result.isEmpty()) {
                    Entry e = result.getFirst();
                    if(e.hasAttribute(attrType)) {
                        matched = evalEntryAttr(e, evalCtx, attrType);
                        if(matched.equals(EnumEvalResult.TRUE)) {
                            stop=true;
                        }
                    }
                }
            }
        }
    }
    return matched.getRet(type, undefined);
    }

    /**
     * This method returns a parent DN based on the level. Not very
     * sophisticated but it works.
     * @param l The level.
     * @param dn The DN to get the parent of.
     * @return Parent DN based on the level or null if the level is greater
     * than the  rdn count.
     */
    private DN getDNParentLevel(int l, DN dn) {
        int rdns=dn.size();
        if(l > rdns) {
            return null;
        }
        DN theDN=dn;
        for(int i=0; i < l;i++) {
            theDN=theDN.parent();
        }
        return theDN;
    }


    /**
     * This method evaluates the user attribute type and calls the correct
     * evalaution method. The three user attribute types that can be selected
     * are USERDN or GROUPDN.
     *
     * @param e The entry to use in the evaluation.
     * @param evalCtx The evaluation context to use in the evaluation.
     * @param attributeType The attribute type to use in the evaluation.
     * @return The result of the evaluation routine.
     */
    private EnumEvalResult evalEntryAttr(Entry e, AciEvalContext evalCtx,
                                         AttributeType attributeType) {
        EnumEvalResult result=EnumEvalResult.FALSE;
        switch (userAttrType) {
            case USERDN: {
                result=UserDN.evaluate(e, evalCtx.getClientDN(),
                                       attributeType);
                break;
            }
            case GROUPDN: {
                result=GroupDN.evaluate(e, evalCtx, attributeType, null);
                break;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public final void toString(StringBuilder buffer)
    {
        buffer.append(super.toString());
    }

}
