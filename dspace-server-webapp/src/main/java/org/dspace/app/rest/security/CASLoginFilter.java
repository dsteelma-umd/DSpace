/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.ShibAuthentication;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class will filter CAS requests to see if the user has been authenticated via CAS.
 * <P>
 * The overall CAS login process is as follows:
 *   1. When CAS plugin is enabled, client/UI receives CAS's absolute URL in WWW-Authenticate header.
 *      See {@link ShibAuthentication} loginPageURL() method.
 *   2. Client sends the user to that URL when they select CAS authentication.
 *   3. User logs in using CAS
 *   4. If successful, they are redirected by CAS to the path where this Filter is "listening" (that path
 *      is passed to CAS as a URL param in step 1)
 *   5. This filter then intercepts the request in order to check for a valid CAS login (see
 *      ShibAuthentication.authenticate()) and stores that user info in a JWT. It also saves that JWT in a *temporary*
 *      authentication cookie.
 *   6. This filter then looks for a "redirectUrl" param (also a part of the original URL from step 1), and redirects
 *      the user to that location (after verifying it's a trusted URL). Usually this is a redirect back to the
 *      Client/UI page where the User started.
 *   7. At that point, the client reads the JWT from the Cookie, and sends it back in a request to /api/authn/login,
 *      which triggers the server-side to destroy the Cookie and move the JWT into a Header
 * <P>
 * This CAS Authentication process is tested in AuthenticationRestControllerIT.
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @author Tim Donohue
 * @see ShibAuthentication
 */
public class CASLoginFilter extends StatelessLoginFilter {
    private static final Logger log = LogManager.getLogger(CASLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public CASLoginFilter(String url, AuthenticationManager authenticationManager,
                          RestAuthenticationService restAuthenticationService) {
        super(url, authenticationManager, restAuthenticationService);
        logger.info("Created CASLoginFilter");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        // First, if CAS is not enabled, throw an immediate ProviderNotFoundException
        // This tells Spring Security that authentication failed
        /*
        if (!CASAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("CAS is disabled.");
        }
         */

        // In the case of CAS, this method does NOT actually authenticate us. The authentication
        // has already happened in CAS. So, this call to "authenticate()" is just triggering
        // CASAuthentication.authenticate() to check for a valid CAS login, and if found, the current user
        // is considered authenticated via CAS.
        // NOTE: because this authentication is implicit, we pass in an empty DSpaceAuthentication
        return authenticationManager.authenticate(new DSpaceAuthentication());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        // Once we've gotten here, we know we have a successful login (i.e. attemptAuthentication() succeeded)

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        log.debug("CAS authentication successful for EPerson {}. Sending back temporary auth cookie",
                  dSpaceAuthentication.getName());
        // OVERRIDE DEFAULT behavior of StatelessLoginFilter to return a temporary authentication cookie containing
        // the Auth Token (JWT). This Cookie is required because we *redirect* the user back to the client/UI after
        // a successful CAS login. Headers cannot be sent via a redirect, so a Cookie must be sent to provide
        // the auth token to the client. On the next request from the client, the cookie is read and destroyed & the
        // Auth token is only used in the Header from that point forward.
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, true);

        // redirect user after completing CAS authentication, sending along the temporary auth cookie
        redirectAfterSuccess(req, res);
    }


    /**
     * After successful login, redirect to the DSpace URL specified by this CAS request (in the "redirectUrl"
     * request parameter). If that 'redirectUrl' is not valid or trusted for this DSpace site, then return a 400 error.
     * @param request
     * @param response
     * @throws IOException
     */
    private void redirectAfterSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get redirect URL from request parameter
        String redirectUrl = request.getParameter("redirectUrl");

        // If redirectUrl unspecified, default to the configured UI
        if (StringUtils.isEmpty(redirectUrl)) {
            // TOFIX - redirect to "dspace.ui.uri"?
            //redirectUrl = configurationService.getProperty("dspace.ui.url");
            redirectUrl = "http://localhost:8080/server/";// configurationService.getProperty("dspace.server.url");
        }

        // Validate that the redirectURL matches either the server or UI hostname. It *cannot* be an arbitrary URL.
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverHostName = Utils.getHostName(configurationService.getProperty("dspace.server.url"));
        ArrayList<String> allowedHostNames = new ArrayList<>();
        allowedHostNames.add(serverHostName);
        String[] allowedUrls = configurationService.getArrayProperty("rest.cors.allowed-origins");
        for (String url : allowedUrls) {
            allowedHostNames.add(Utils.getHostName(url));
        }

        if (StringUtils.equalsAnyIgnoreCase(redirectHostName, allowedHostNames.toArray(new String[0]))) {
            log.debug("CAS redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("Invalid CAS redirectURL=" + redirectUrl +
                          ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid redirectURL! Must match server or ui hostname.");
        }
    }
}
