package com.cadac.stone_inscription.post.mapper;

import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.post.dto.InscriptionPostDto;

public class PostMapper {

    // ---------- DTO -> Entity ----------
    public static InscriptionPost toEntity(InscriptionPostDto dto) {
        if (dto == null) {
            return null;
        }

        return InscriptionPost.builder()
                .topic(dto.getTopic())
                .script(dto.getScript())
                .type(dto.getType())
                .description(toEntityDescription(dto.getDescription()))
                .build();
    }

    public static InscriptionPost.Description toEntityDescription(InscriptionPostDto.DescriptionDto dto) {
        if (dto == null) {
            return null;
        }

        return InscriptionPost.Description.builder()
                .title(dto.getTitle())
                .subject(dto.getSubject())
                .description(dto.getDescription())
                .scriptLanguage(dto.getScriptLanguage())
                .language(dto.getLanguage())
                .upvote(0) // default
                .build();
    }

    // ---------- Entity -> DTO ----------
    public static InscriptionPostDto toDto(InscriptionPost entity) {
        if (entity == null) {
            return null;
        }

        return InscriptionPostDto.builder()
                .topic(entity.getTopic())
                .script(entity.getScript())
                .type(entity.getType())
                .description(toDtoDescription(entity.getDescription()))
                .build();
    }

    private static InscriptionPostDto.DescriptionDto toDtoDescription(InscriptionPost.Description entity) {
        if (entity == null) {
            return null;
        }

        return InscriptionPostDto.DescriptionDto.builder()
                .title(entity.getTitle())
                .subject(entity.getSubject())
                .description(entity.getDescription())
                .scriptLanguage(entity.getScriptLanguage())
                .language(entity.getLanguage())
                .build();
    }
}
