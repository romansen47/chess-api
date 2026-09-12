package demo.chess.api.mapper;

import demo.chess.api.dto.MoveAnnotationDto;
import demo.chess.analysis.annotation.ExtraordinaryReason;
import demo.chess.analysis.annotation.MaterialSacrificeType;
import demo.chess.analysis.annotation.MoveAnnotation;
import demo.chess.analysis.annotation.MoveAnnotationKind;

/**
 * Maps chess-core annotation domain objects to the stable API representation.
 */
public final class MoveAnnotationDtoMapper {

    private MoveAnnotationDtoMapper() {
    }

    public static MoveAnnotationDto toDto(MoveAnnotation annotation) {
        if (annotation == null) {
            return null;
        }

        MoveAnnotationDto dto = new MoveAnnotationDto();
        dto.setSymbol(symbol(annotation.getKind()));
        dto.setKind(kind(annotation.getKind()));
        dto.setWinChanceLoss(annotation.getWinChanceLoss());
        dto.setBestEvaluation(annotation.getBestEvaluation());
        dto.setSecondBestEvaluation(annotation.getSecondBestEvaluation());

        dto.setExtraordinaryReason(
                reason(annotation.getExtraordinaryReason()));
        dto.setMaterialInvestment(
                annotation.getMaterialInvestment());
        dto.setSacrificeType(
                sacrificeType(annotation.getSacrificeType()));

        dto.setEarlyDepth(annotation.getEarlyDepth());
        dto.setEarlyRank(annotation.getEarlyRank());
        dto.setFinalDepth(annotation.getFinalDepth());
        dto.setFinalRank(annotation.getFinalRank());
        dto.setGivesCheck(annotation.getGivesCheck());
        dto.setEarlyRegret(annotation.getEarlyRegret());
        dto.setEarlyStrength(annotation.getEarlyStrength());
        dto.setFinalStrength(annotation.getFinalStrength());
        return dto;
    }

    private static String symbol(MoveAnnotationKind kind) {
        if (kind == null) {
            return "";
        }
        return switch (kind) {
            case ONLY_MOVE -> "!";
            case EXTRAORDINARY -> "!!";
            case MISTAKE -> "?";
            case BLUNDER -> "??";
        };
    }

    private static String kind(MoveAnnotationKind kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case ONLY_MOVE -> "onlyMove";
            case EXTRAORDINARY -> "extraordinary";
            case MISTAKE -> "mistake";
            case BLUNDER -> "blunder";
        };
    }

    private static String sacrificeType(MaterialSacrificeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case ACTIVE_INVESTMENT -> "activeInvestment";
            case NEW_MATERIAL_OFFER -> "newMaterialOffer";
            case DECLINED_MATERIAL_SAVE -> "declinedMaterialSave";
        };
    }

    private static String reason(ExtraordinaryReason reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason) {
            case DEEP_DISCOVERY -> "deepDiscovery";
            case MATERIAL_SACRIFICE -> "materialSacrifice";
            case DEEP_DISCOVERY_AND_MATERIAL_SACRIFICE ->
                    "deepDiscoveryAndMaterialSacrifice";
        };
    }
}
