package org.dspace.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.AbstractDSpaceTest;
import org.junit.Before;
import org.junit.Test;

public class DCInputTest extends AbstractDSpaceTest {
    private Map<String, String> fieldMap;
    private Map<String, List<String>> listMap;

    @Before
    public void setUp() {
        fieldMap = new HashMap<>();
        listMap = new HashMap<>();
    }
    @Test
    public void testTypeBind_NoParameters() {
        DCInput dcInput = new DCInput(fieldMap, listMap);
        assertThat(dcInput.getTypeBindList(), is(empty()));
    }

    @Test
    public void testTypeBind_SingleType() {
        fieldMap.put("type-bind", "Book");
        DCInput dcInput = new DCInput(fieldMap, listMap);
        assertThat(dcInput.getTypeBindList(), contains("Book"));
    }

    @Test
    public void testTypeBind_MultipleTypes_CommaSeparatedIsDefault() {
        fieldMap.put("type-bind", "Article, Book");
        DCInput dcInput = new DCInput(fieldMap, listMap);
        assertThat(dcInput.getTypeBindList(), contains("Article", "Book"));
    }

    @Test
    public void testTypeBind_MultipleTypesColonSeparated() {
        fieldMap.put("type-bind", "Article:Book");
        fieldMap.put("type-bind-separator", ":");
        DCInput dcInput = new DCInput(fieldMap, listMap);
        assertThat(dcInput.getTypeBindList(), contains("Article", "Book"));
    }

    @Test
    public void testTypeBind_MultipleTypesColonSeparated_TypeContainsComma() {
        fieldMap.put("type-bind", "Article:Book:Image, 3-D:Map");
        fieldMap.put("type-bind-separator", ":");
        DCInput dcInput = new DCInput(fieldMap, listMap);
        assertThat(dcInput.getTypeBindList(), contains(
            "Article", "Book", "Image, 3-D", "Map"));
    }
}
