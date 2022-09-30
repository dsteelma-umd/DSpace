/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.LdapInfoRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import edu.umd.lib.dspace.authenticate.Ldap;
import edu.umd.lib.dspace.authenticate.impl.LdapImpl;
import edu.umd.lib.dspace.authenticate.impl.LdapInfo;

/**
 * Link repository for the direct LdapInfo subresource of an individual eperson.
 */
@Component(EPersonRest.CATEGORY + "." + EPersonRest.NAME + "." + EPersonRest.LDAP_INFO)
public class EPersonLdapInfoLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {
    private static final Logger log = LoggerFactory.getLogger(EPersonLdapInfoLinkRepository.class);

    @Autowired
    EPersonService epersonService;

    // @Autowired
    // LdapService groupService;

    @PreAuthorize("hasPermission(#epersonId, 'EPERSON', 'READ')")
    public LdapInfoRest getLdapInfo(@Nullable HttpServletRequest request,
            UUID epersonId,
            @Nullable Pageable optionalPageable,
            Projection projection) {
        try {
            Context context = obtainContext();
            EPerson eperson = epersonService.find(context, epersonId);
            if (eperson == null) {
                throw new ResourceNotFoundException("No such eperson: " + epersonId);
            }

            String netId = eperson.getNetid();
            if (netId == null) {
                return null;
            }

            try (Ldap ldap = new LdapImpl(context)) {
                log.debug("Querying LDAP for netId={}", netId);
                LdapInfo ldapInfo = ldap.queryLdap(netId);

                if (ldapInfo == null) {
                    log.debug("No LDAP information found for netId={}", netId);
                    return null;
                }

                log.debug("LDAP information found for netID={}", netId);

                LdapInfoRest ldapInfoRest = new LdapInfoRest();

                ldapInfoRest.setFirstName(ldapInfo.getFirstName());
                ldapInfoRest.setLastName(ldapInfo.getLastName());
                ldapInfoRest.setPhone(ldapInfo.getPhone());
                ldapInfoRest.setEmail(ldapInfo.getEmail());

                ldapInfoRest.setIsFaculty(ldapInfo.isFaculty());
                ldapInfoRest.setUmAppointments(ldapInfo.getAttributeAll("umappointment"));
                List<GroupRest> groupList = ldapInfo.getGroups(context).stream()
                    .map(g -> (GroupRest) converter.toRest(g, projection)).collect(Collectors.toList());
                ldapInfoRest.setGroups(groupList);
                return ldapInfoRest;
            } catch (NamingException ne) {
                log.error("Exception accessing LDAP. netId=" + netId, ne);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
