package com.cnh.ies.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;

/**
 * Flattens nested role permissions from session {@link UserInfo} for JWT claims, frontend action lists,
 * and service-layer authorization checks.
 */
public final class PermissionUtils {

    private PermissionUtils() {}

    /**
     * Deduplicated permissions from all roles, sorted by code.
     */
    public static List<PermissionInfo> flattenPermissions(UserInfo userInfo) {
        if (userInfo == null || userInfo.getRoles() == null) {
            return List.of();
        }
        Map<String, PermissionInfo> byCode = new LinkedHashMap<>();
        for (RoleInfo role : userInfo.getRoles()) {
            if (role.getPermissions() == null) {
                continue;
            }
            for (PermissionInfo p : role.getPermissions()) {
                if (p.getCode() != null && !p.getCode().isBlank()) {
                    byCode.putIfAbsent(p.getCode().trim(), p);
                }
            }
        }
        return byCode.values().stream()
                .sorted(Comparator.comparing(PermissionInfo::getCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    public static Set<String> permissionCodes(UserInfo userInfo) {
        return flattenPermissions(userInfo).stream()
                .map(PermissionInfo::getCode)
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public static boolean hasPermission(UserInfo userInfo, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissionCodes(userInfo).contains(permissionCode.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean hasAnyPermission(UserInfo userInfo, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        Set<String> owned = permissionCodes(userInfo);
        for (String code : permissionCodes) {
            if (code != null && owned.contains(code.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean canActOnPaymentApprovalStage(UserInfo userInfo, String approvalRole) {
        String required = requiredPaymentPermissionForStage(approvalRole);
        return required != null && hasPermission(userInfo, required);
    }

    public static boolean canActOnWarehouseInboundApprovalStage(UserInfo userInfo, String approvalRole) {
        String required = requiredWarehouseInboundPermissionForStage(approvalRole);
        return required != null && hasPermission(userInfo, required);
    }

    public static boolean canActOnWarehouseOutboundApprovalStage(UserInfo userInfo, String approvalRole) {
        String required = requiredWarehouseOutboundPermissionForStage(approvalRole);
        return required != null && hasPermission(userInfo, required);
    }

    public static String requiredPaymentPermissionForStage(String approvalRole) {
        return requiredPermissionForStage(
                approvalRole,
                PermissionConstants.PAYMENT_APPROVE_LEVEL_1,
                PermissionConstants.PAYMENT_APPROVE_LEVEL_2,
                PermissionConstants.PAYMENT_APPROVE_FINAL);
    }

    public static String requiredWarehouseInboundPermissionForStage(String approvalRole) {
        return requiredPermissionForStage(
                approvalRole,
                PermissionConstants.WAREHOUSE_INBOUND_APPROVE_LEVEL_1,
                PermissionConstants.WAREHOUSE_INBOUND_APPROVE_LEVEL_2,
                PermissionConstants.WAREHOUSE_INBOUND_APPROVE_FINAL);
    }

    public static String requiredWarehouseOutboundPermissionForStage(String approvalRole) {
        return requiredPermissionForStage(
                approvalRole,
                PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1,
                PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2,
                PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_FINAL);
    }

    private static String requiredPermissionForStage(
            String approvalRole,
            String level1,
            String level2,
            String levelFinal) {
        if (approvalRole == null || approvalRole.isBlank()) {
            return null;
        }
        return switch (approvalRole.trim().toUpperCase(Locale.ROOT)) {
            case PermissionConstants.APPROVAL_STAGE_ACCOUNTANT -> level1;
            case PermissionConstants.APPROVAL_STAGE_HEAD_ACCOUNTANT -> level2;
            case PermissionConstants.APPROVAL_STAGE_FINAL_APPROVER -> levelFinal;
            case "ADMIN" -> levelFinal;
            case "ACCOUNTANT_MANAGER" -> level2;
            default -> null;
        };
    }
}
