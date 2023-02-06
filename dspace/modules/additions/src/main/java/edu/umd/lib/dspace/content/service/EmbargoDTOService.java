package edu.umd.lib.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import edu.umd.lib.dspace.content.EmbargoDTO;
import org.dspace.core.Context;


/**
 * Service interface class for the EmbargoDTO object.
 *
 * The implementation of this class is responsible for all business logic calls
 * for the EmbargoDTO object and is autowired by spring
 */
public interface EmbargoDTOService {
    /**
     * Return the list of embargoed items.
     *
     * @return embargoList List of EmbargoDTO object
     */
    public List<EmbargoDTO> getEmbargoList(Context context) throws SQLException;

    /**
     * Returns the embargo for the given bitstream UUID.
     *
     * @param bitstreamUuid the bitstream UUID to return the embargo of
     * @return an EmbargoDTO object describing the embargo for the given UUID
     */
    public EmbargoDTO getEmbargo(Context context, UUID bitstreamUuid) throws SQLException;
}
