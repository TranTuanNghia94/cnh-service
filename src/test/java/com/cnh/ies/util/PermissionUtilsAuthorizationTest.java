package com.cnh.ies.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;

class PermissionUtilsAuthorizationTest {

    @Test
    void canActOnPaymentApprovalStage_mapsWorkflowLabelsToPermissions() {
        UserInfo accountant = userWithPermissions(PermissionConstants.PAYMENT_APPROVE_LEVEL_1);
        UserInfo head = userWithPermissions(PermissionConstants.PAYMENT_APPROVE_LEVEL_2);
        UserInfo admin = userWithPermissions(PermissionConstants.PAYMENT_APPROVE_FINAL);

        assertTrue(PermissionUtils.canActOnPaymentApprovalStage(accountant, "ACCOUNTANT"));
        assertFalse(PermissionUtils.canActOnPaymentApprovalStage(accountant, "HEAD_ACCOUNTANT"));

        assertTrue(PermissionUtils.canActOnPaymentApprovalStage(head, "HEAD_ACCOUNTANT"));
        assertFalse(PermissionUtils.canActOnPaymentApprovalStage(head, "FINAL_APPROVER"));

        assertTrue(PermissionUtils.canActOnPaymentApprovalStage(admin, "FINAL_APPROVER"));
        assertTrue(PermissionUtils.canActOnPaymentApprovalStage(admin, "ADMIN"));
    }

    @Test
    void canActOnWarehouseInboundApprovalStage_mapsWorkflowLabelsToPermissions() {
        UserInfo level1 = userWithPermissions(PermissionConstants.WAREHOUSE_INBOUND_APPROVE_LEVEL_1);
        UserInfo level2 = userWithPermissions(PermissionConstants.WAREHOUSE_INBOUND_APPROVE_LEVEL_2);
        UserInfo levelFinal = userWithPermissions(PermissionConstants.WAREHOUSE_INBOUND_APPROVE_FINAL);

        assertTrue(PermissionUtils.canActOnWarehouseInboundApprovalStage(level1, "ACCOUNTANT"));
        assertTrue(PermissionUtils.canActOnWarehouseInboundApprovalStage(level2, "HEAD_ACCOUNTANT"));
        assertTrue(PermissionUtils.canActOnWarehouseInboundApprovalStage(levelFinal, "FINAL_APPROVER"));
        assertEquals(
                PermissionConstants.WAREHOUSE_INBOUND_APPROVE_LEVEL_2,
                PermissionUtils.requiredWarehouseInboundPermissionForStage("ACCOUNTANT_MANAGER"));
    }

    @Test
    void canActOnWarehouseOutboundApprovalStage_mapsWorkflowLabelsToPermissions() {
        UserInfo level1 = userWithPermissions(PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1);
        UserInfo levelFinal = userWithPermissions(PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_FINAL);

        assertTrue(PermissionUtils.canActOnWarehouseOutboundApprovalStage(level1, "ACCOUNTANT"));
        assertFalse(PermissionUtils.canActOnWarehouseOutboundApprovalStage(level1, "FINAL_APPROVER"));
        assertTrue(PermissionUtils.canActOnWarehouseOutboundApprovalStage(levelFinal, "FINAL_APPROVER"));
    }

    @Test
    void hasAnyPermission_checksFlattenedCodes() {
        UserInfo user = userWithPermissions(
                PermissionConstants.PAYMENT_READ,
                PermissionConstants.PAYMENT_UPLOAD_BANK_NOTE);

        assertTrue(PermissionUtils.hasAnyPermission(
                user,
                PermissionConstants.PAYMENT_APPROVE_LEVEL_1,
                PermissionConstants.PAYMENT_UPLOAD_BANK_NOTE));
        assertFalse(PermissionUtils.hasPermission(user, PermissionConstants.PAYMENT_APPROVE_LEVEL_1));
    }

    private static UserInfo userWithPermissions(String... codes) {
        Set<PermissionInfo> permissions = java.util.Arrays.stream(codes)
                .map(code -> {
                    PermissionInfo p = new PermissionInfo();
                    p.setId(UUID.randomUUID());
                    p.setCode(code);
                    return p;
                })
                .collect(java.util.stream.Collectors.toSet());

        RoleInfo role = new RoleInfo();
        role.setCode("TEST");
        role.setPermissions(permissions);

        UserInfo user = new UserInfo();
        user.setId(UUID.randomUUID());
        user.setUsername("tester");
        user.setRoles(Set.of(role));
        return user;
    }
}
