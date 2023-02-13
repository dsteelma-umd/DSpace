package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for BitstreamConverter
 *
 * Using an integration test of ResourcePolicyService dependency
 */
public class BitstreamConverterIT extends AbstractControllerIntegrationTest {
    @Autowired
    private ConverterService converterService;

    private ResourcePolicy etdEmbargoPolicy;
    private ResourcePolicy otherPolicy;
    private Bitstream mockBitstream;
    private BitstreamConverter converter;
    private SimpleDateFormat asDate;

    @Before
    public void setup() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        Group etdEmbargoGroup = GroupBuilder.createGroup(context).withName("ETD Embargo").build();
        etdEmbargoPolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                            .withGroup(etdEmbargoGroup).build();

        Group otherGroup = GroupBuilder.createGroup(context).withName("Other").build();
        otherPolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                        .withGroup(otherGroup).build();

        mockBitstream = mock(Bitstream.class);
        context.restoreAuthSystemState();

        // Need to retrieve BitstreamConverter via converter service so that
        // all the Autowired resources in the converter get initialized.
        // This seems to be due to ConverterService being responible for
        // autowiring the converters (see comment in DSpaceObjectConverter)
        converter = (BitstreamConverter) ((DSpaceObjectConverter)
             converterService.getConverter(Bitstream.class));
        asDate = new SimpleDateFormat("yyyy-MM-dd");
    }

    @Test(expected = NullPointerException.class)
    public void getRestrictedAccess_throwsNullPointerExceptionWhenBitstreamIsNull() {
        // Method assumes that provided DSpaceObject is always non-null.
        converter.getRestrictedAccess(null);

    }

    @Test
    public void testGetRestrictedAccess_ReturnsNone_WhenNoResourcePolicies() {
        assertEquals("NONE", converter.getRestrictedAccess(mockBitstream));
    }

    @Test
    public void testGetRestrictedAccess_ReturnsNone_WhenNoEtdEmbargoPolicy() {
        when(mockBitstream.getResourcePolicies()).thenReturn(List.of(otherPolicy));
        assertEquals("NONE", converter.getRestrictedAccess(mockBitstream));
    }

    @Test
    public void testGetRestrictedAccess_ReturnsNone_WhenEtdEmbargoPolicyWithEndDateInPast() throws Exception {
        etdEmbargoPolicy.setEndDate(asDate.parse("1972-12-03"));
        when(mockBitstream.getResourcePolicies()).thenReturn(List.of(etdEmbargoPolicy));

        assertEquals("NONE", converter.getRestrictedAccess(mockBitstream));
    }

    @Test
    public void testGetRestrictedAccess_ReturnsEndDate_WhenEtdEmbargoPolicyWithEndDateInFuture() throws Exception {
        Date futureDate = new Date(System.currentTimeMillis() + (356 * 24 * 60)); // One year (approx.) in the future
        String expectedDateStr = asDate.format(futureDate);
        etdEmbargoPolicy.setEndDate(futureDate);
        when(mockBitstream.getResourcePolicies()).thenReturn(List.of(etdEmbargoPolicy));

        assertEquals(expectedDateStr, converter.getRestrictedAccess(mockBitstream));
    }

    @Test
    public void testGetRestrictedAccess_ReturnsForever_WhenEtdEmbargoPolicyHasNullEndDate() throws Exception {
        etdEmbargoPolicy.setEndDate(null);
        when(mockBitstream.getResourcePolicies()).thenReturn(List.of(etdEmbargoPolicy));

        assertEquals("FOREVER", converter.getRestrictedAccess(mockBitstream));
    }
}
