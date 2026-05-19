package com.cadac.stone_inscription.content.delete;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentDeleteResult {
    int archivedPosts;
    int archivedComments;
    int deletedImages;
}
