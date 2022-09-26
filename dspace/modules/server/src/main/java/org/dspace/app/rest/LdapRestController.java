package org.dspace.app.rest;


import org.dspace.app.rest.model.LdapRest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LdapRestController {
  @PreAuthorize("hasAuthority('ADMIN')")
  @RequestMapping(value = "/api/ldap/{userId}", method = RequestMethod.GET)
  public ResponseEntity<?> queryLdap(@PathVariable String userId) {
    LdapRest ldapRest = new LdapRest();
    ldapRest.setName(userId);

    return new ResponseEntity<> (ldapRest, HttpStatus.OK);
  }
}
