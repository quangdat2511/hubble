package com.example.hubble.view.search;

import com.example.hubble.view.search.SearchResultFragment.Category;
import com.example.hubble.viewmodel.SearchViewModel.ScopeType;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SearchTabConfigTest {

    @Test
    public void channelScope_hasMembersAndChannelsBaseTabs() {
        List<Category> tabs = SearchTabConfig.getBaseCategories(ScopeType.CHANNEL);
        assertEquals(List.of(Category.MEMBERS, Category.CHANNELS), tabs);
    }

    @Test
    public void serverScope_hasMembersAndChannelsBaseTabs() {
        List<Category> tabs = SearchTabConfig.getBaseCategories(ScopeType.SERVER);
        assertEquals(List.of(Category.MEMBERS, Category.CHANNELS), tabs);
    }

    @Test
    public void dmScope_hasFriendsBaseTab() {
        List<Category> tabs = SearchTabConfig.getBaseCategories(ScopeType.DM);
        assertEquals(List.of(Category.FRIENDS), tabs);
    }
}
