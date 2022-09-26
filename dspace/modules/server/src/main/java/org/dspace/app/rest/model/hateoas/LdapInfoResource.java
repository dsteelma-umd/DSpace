package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.LdapInfoRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(LdapInfoRest.NAME)
public class LdapInfoResource extends DSpaceResource<LdapInfoRest> {
    public LdapInfoResource(LdapInfoRest ldapInfo, Utils utils) {
        super(ldapInfo, utils);
    }
}
