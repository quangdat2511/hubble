package com.example.hubble.adapter.search;

import com.example.hubble.data.model.search.SearchMemberDto;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchMemberSectionsTest {

    @Test
    public void cluster_putsMeSectionFirstWhenSelfExists() throws Exception {
        SearchMemberDto me = member("u1", "me", "Me User", true);
        SearchMemberDto other = member("u2", "alice", "Alice", false);

        List<SearchMemberSections.Item> clustered = SearchMemberSections.cluster(List.of(other, me));

        assertTrue(clustered.get(0).isHeader);
        assertEquals("Me", clustered.get(0).header);
        assertEquals("u1", clustered.get(1).member.getId());
    }

    @Test
    public void cluster_groupsOthersAlphabeticallyAfterMe() throws Exception {
        SearchMemberDto me = member("u1", "me", "Me User", true);
        SearchMemberDto bob = member("u3", "bob", "Bob", false);
        SearchMemberDto alice = member("u2", "alice", "Alice", false);

        List<SearchMemberSections.Item> clustered = SearchMemberSections.cluster(List.of(bob, me, alice));

        assertEquals("A", clustered.get(2).header);
        assertEquals("u2", clustered.get(3).member.getId());
        assertEquals("B", clustered.get(4).header);
        assertEquals("u3", clustered.get(5).member.getId());
    }

    private SearchMemberDto member(String id, String username, String displayName, boolean isSelf) throws Exception {
        SearchMemberDto dto = new SearchMemberDto();
        setField(dto, "id", id);
        setField(dto, "username", username);
        setField(dto, "displayName", displayName);
        setField(dto, "isSelf", isSelf);
        return dto;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
