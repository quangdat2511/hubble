package com.example.hubble.adapter.search;

import androidx.annotation.Nullable;

import com.example.hubble.data.model.search.SearchMemberDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SearchMemberSections {
    private SearchMemberSections() {}

    public static final class Item {
        public final boolean isHeader;
        @Nullable public final String header;
        @Nullable public final SearchMemberDto member;

        private Item(boolean isHeader, @Nullable String header, @Nullable SearchMemberDto member) {
            this.isHeader = isHeader;
            this.header = header;
            this.member = member;
        }

        public static Item header(String label) {
            return new Item(true, label, null);
        }

        public static Item member(SearchMemberDto value) {
            return new Item(false, null, value);
        }
    }

    public static List<Item> cluster(List<SearchMemberDto> members) {
        List<Item> result = new ArrayList<>();
        if (members == null || members.isEmpty()) {
            return result;
        }

        List<SearchMemberDto> meList  = new ArrayList<>();
        List<SearchMemberDto> others  = new ArrayList<>();
        for (SearchMemberDto m : members) {
            if (m.isSelf()) meList.add(m); else others.add(m);
        }

        Comparator<SearchMemberDto> byName = Comparator.comparing(
                m -> (m.getDisplayName() != null ? m.getDisplayName() : m.getUsername()),
                String.CASE_INSENSITIVE_ORDER);
        meList.sort(byName);
        others.sort(byName);

        if (!meList.isEmpty()) {
            result.add(Item.header("Me"));
            for (SearchMemberDto m : meList) result.add(Item.member(m));
        }

        String currentLetter = null;
        for (SearchMemberDto m : others) {
            String name = m.getDisplayName() != null ? m.getDisplayName() : m.getUsername();
            String letter = (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : "#";
            if (!letter.equals(currentLetter)) {
                currentLetter = letter;
                result.add(Item.header(letter));
            }
            result.add(Item.member(m));
        }
        return result;
    }
}
