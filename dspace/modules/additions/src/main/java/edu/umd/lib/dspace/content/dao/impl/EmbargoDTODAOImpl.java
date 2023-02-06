package edu.umd.lib.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.DateType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves the list of embargoed items from the database.
 */
public class EmbargoDTODAOImpl extends AbstractHibernateDAO<Object> implements EmbargoDTODAO {

    private static Logger log = LoggerFactory.getLogger(EmbargoDTODAOImpl.class);

    private static Map<String, Type> scalarsMap = Map.of(
        "handle", StringType.INSTANCE,
        "itemId", PostgresUUIDType.INSTANCE,
        "bitstreamId", PostgresUUIDType.INSTANCE,
        "title", StringType.INSTANCE,
        "advisor", StringType.INSTANCE,
        "author", StringType.INSTANCE,
        "department", StringType.INSTANCE,
        "type", StringType.INSTANCE,
        "endDate", DateType.INSTANCE
    );

    private static final String embargoListSQL =
        "SELECT DISTINCT ON (h.handle) h.handle as handle, "
        + "i1.uuid as itemId, bs.uuid as bitstreamId, "
        + "(SELECT dc.text_value FROM metadatavalue dc "
        + "WHERE dc.metadata_field_id=:titleId AND dc.dspace_object_id=i1.uuid LIMIT 1) as title, "
        + "(SELECT dc.text_value FROM metadatavalue dc "
        + "WHERE dc.metadata_field_id=:advisorId AND dc.dspace_object_id=i1.uuid LIMIT 1) as advisor, "
        + "(SELECT dc.text_value FROM metadatavalue dc "
        + "WHERE dc.metadata_field_id=:authorId AND dc.dspace_object_id=i1.uuid LIMIT 1) as author, "
        + "(SELECT dc.text_value FROM metadatavalue dc "
        + "WHERE dc.metadata_field_id=:departmentId AND dc.dspace_object_id=i1.uuid LIMIT 1) as department, "
        + "(SELECT dc.text_value FROM metadatavalue dc "
        + "WHERE dc.metadata_field_id=:typeId AND dc.dspace_object_id=i1.uuid LIMIT 1) as type, "
        + "rp.end_date as endDate "
        + "FROM handle h, item i1, item2bundle i2b1, bundle2bitstream b2b1, bitstream bs, "
        + "resourcepolicy rp, epersongroup g, metadatavalue mv "
        + "WHERE h.resource_id=i1.uuid AND i1.uuid=i2b1.item_id AND i2b1.bundle_id=b2b1.bundle_id AND "
        + "b2b1.bitstream_id=bs.uuid AND bs.uuid=rp.dspace_object AND (rp.end_date > CURRENT_DATE "
        + "OR rp.end_date IS NULL) AND rp.epersongroup_id = g.uuid AND "
        + "g.uuid = mv.dspace_object_id AND mv.text_value = :groupName";

    protected EmbargoDTODAOImpl() {
        super();
    }

    @Override
    public List<EmbargoDTO> getEmbargoDTOList(Context context, int titleId, int advisorId, int authorId,
            int departmentId, int typeId, String groupName) throws SQLException {

        log.debug("Getting Embargo List with params titleId: {}, advisorId: {}, authorId: {}, " +
                "departmentId: {}, typeId: {}, groupName: {}", titleId, advisorId,
                authorId, departmentId, typeId, groupName);

        Map<String, Object> parametersMap = Map.of(
            "titleId", titleId,
            "advisorId", advisorId,
            "authorId", authorId,
            "departmentId", departmentId,
            "typeId", typeId,
            "groupName", groupName
        );

        NativeQuery<EmbargoDTO> nativeQuery =
            (NativeQuery<EmbargoDTO>) createSQLQuery(context, embargoListSQL, scalarsMap, parametersMap);
        Query<EmbargoDTO> sqlQuery =
            (Query<EmbargoDTO>) nativeQuery.setResultTransformer(Transformers.aliasToBean(EmbargoDTO.class));

        return (List<EmbargoDTO>) sqlQuery.list();
    }

    @Override
    public EmbargoDTO getEmbargoDTO(
        Context context, UUID bitstreamUuid, int titleId, int advisorId, int authorId, int departmentId,
            int typeId, String groupName) throws SQLException {

        log.debug("Getting Embargo with params bitstreamUuid: {}, titleId: {}, advisorId: {}, authorId: {}, " +
                 "departmentId: {}, typeId: {}, groupName: {}", bitstreamUuid, titleId, advisorId,
                authorId, departmentId, typeId, groupName);

        Map<String, Object> parametersMap = Map.of(
            "titleId", titleId,
            "advisorId", advisorId,
            "authorId", authorId,
            "departmentId", departmentId,
            "typeId", typeId,
            "groupName", groupName,
            "bitstreamUuid", bitstreamUuid
        );

        String sql = embargoListSQL + " AND bs.uuid=:bitstreamUuid";

        NativeQuery<EmbargoDTO> nativeQuery =
            (NativeQuery<EmbargoDTO>) createSQLQuery(context, sql, scalarsMap, parametersMap);
        Query<EmbargoDTO> sqlQuery =
            (Query<EmbargoDTO>) nativeQuery.setResultTransformer(Transformers.aliasToBean(EmbargoDTO.class));

        return (EmbargoDTO) sqlQuery.uniqueResult();
    }


    @SuppressWarnings("unchecked")
    private NativeQuery<EmbargoDTO> createSQLQuery(
        Context context, String sql, Map<String, Type> scalarsMap, Map<String, Object> parametersMap)
        throws SQLException {

        NativeQuery<EmbargoDTO> nativeQuery =
            (NativeQuery<EmbargoDTO>) getHibernateSession(context).createSQLQuery(sql);
        for (Entry<String, Type> entry: scalarsMap.entrySet()) {
            nativeQuery.addScalar(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Object> entry: parametersMap.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        return nativeQuery;
    }
}
