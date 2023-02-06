package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST/HAL Browser endpoint for retrieving a list of embargoed items.
 */
@RestController
public class EmbargoRestController implements InitializingBean {
    @Autowired
    private EmbargoDTOService embargoService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping("/api/embargo-list")
    public List<EmbargoDTO> embargoList(HttpServletResponse response, HttpServletRequest request) throws SQLException {
        Context context = obtainContext(request);
        List<EmbargoDTO> embargoes = embargoService.getEmbargoList(context);

        return embargoes;
    }

    // Does not require authorization, because we need to access anonymously
    // for Angular "Restricted Access" functionality.
    @RequestMapping("/api/embargo-list/{uuid}")
    public EmbargoDTO embargoForBitstream(
        @PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
        throws SQLException {
        Context context = obtainContext(request);
        EmbargoDTO embargo = embargoService.getEmbargo(context, uuid);

        return embargo;
    }

    @Override
    public void afterPropertiesSet() {
        List<Link> links = List.of(Link.of("/api/embargo-list", "embargo-list"));
        discoverableEndpointsService.register(this, links);
    }
}
