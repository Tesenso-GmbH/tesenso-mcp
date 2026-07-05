package org.thingsboard.ai.mcp.server.tools.tesenso;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import java.util.Map;

import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_SIZE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.TEXT_SEARCH_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.UUID_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.pageParams;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.params;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotBlank;

/**
 * Tesenso contracts, billing periods (Abrechnungsperioden) and invoices.
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-billing")
public class TesensoBillingTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to list Tesenso contracts (Verträge) with pagination and optional text search. " +
            "Contracts carry billing configuration (currency, tax, QR-IBAN) and link units, meters and billing periods.")
    public String getTesensoContracts(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/contracts", pageParams(pageSize, page, textSearch, null, null));
    }

    @Tool(description = "Use this to get one Tesenso contract by id.")
    public String getTesensoContractById(
            @ToolParam(description = "Contract id. " + UUID_DESC) @NotBlank String contractId) {
        return api.get("/api/tesenso/contract/" + contractId);
    }

    @Tool(description = "Use this to list Tesenso contracts by status. " +
            "Allowed values: 'DRAFT', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'EXPIRED'.")
    public String getTesensoContractsByStatus(
            @ToolParam(description = "Contract status: DRAFT, ACTIVE, SUSPENDED, TERMINATED or EXPIRED.")
            @NotBlank String status) {
        return api.get("/api/tesenso/contracts/status/" + status);
    }

    @Tool(description = "Use this to list all billing periods (Abrechnungsperioden) of a contract.")
    public String getBillingPeriods(
            @ToolParam(description = "Contract id. " + UUID_DESC) @NotBlank String contractId) {
        return api.get("/api/tesenso/contract/" + contractId + "/billing-periods");
    }

    @Tool(description = "Use this to get one billing period by id, including period range, status, advance payments and totals.")
    public String getBillingPeriodById(
            @ToolParam(description = "Billing period id. " + UUID_DESC) @NotBlank String billingPeriodId) {
        return api.get("/api/tesenso/billing-period/" + billingPeriodId);
    }

    @Tool(description = "Use this to get the 5-step yearly billing progress checklist of a billing period " +
            "(readings, allocation, preflight, produce, sent).")
    public String getBillingPeriodYearlyProgress(
            @ToolParam(description = "Billing period id. " + UUID_DESC) @NotBlank String billingPeriodId) {
        return api.get("/api/tesenso/billing-period/" + billingPeriodId + "/yearly-progress");
    }

    @Tool(description = "Use this to run the go/no-go preflight check of a billing period before producing statements. " +
            "RED findings block production, YELLOW are warnings. Read-only check, changes nothing.")
    public String runBillingPeriodPreflight(
            @ToolParam(description = "Billing period id. " + UUID_DESC) @NotBlank String billingPeriodId) {
        return api.post("/api/tesenso/billing-period/" + billingPeriodId + "/preflight", null);
    }

    @Tool(description = "Use this to get the allocation results (per-unit cost allocation) of a billing period.")
    public String getAllocationResults(
            @ToolParam(description = "Billing period id. " + UUID_DESC) @NotBlank String periodId) {
        return api.get("/api/tesenso/billing/periods/" + periodId + "/results");
    }

    @Tool(description = "Use this to get aggregated subscription and revenue metrics of the tenant: " +
            "MRR, ARR, subscription counts by status, revenue breakdown and upcoming renewals.")
    public String getBillingMetrics() {
        return api.get("/api/tesenso/billing/dashboard/metrics");
    }

    @Tool(description = "Use this to list Tesenso invoices with pagination and optional text search. " +
            "Sortable by createdTime, invoiceNumber, invoiceDate, dueDate or totalAmount.")
    public String getTesensoInvoices(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch,
            @ToolParam(required = false, description = "Sort property: createdTime, invoiceNumber, invoiceDate, dueDate or totalAmount.")
            String sortProperty,
            @ToolParam(required = false, description = "Sort order: 'ASC' or 'DESC'.") String sortOrder) {
        return api.get("/api/tesenso/invoice/list", pageParams(pageSize, page, textSearch, sortProperty, sortOrder));
    }

    @Tool(description = "Use this to get one Tesenso invoice by id.")
    public String getTesensoInvoiceById(
            @ToolParam(description = "Invoice id. " + UUID_DESC) @NotBlank String invoiceId) {
        return api.get("/api/tesenso/invoice/" + invoiceId);
    }

    @Tool(description = "Use this to list all overdue Tesenso invoices of the tenant.")
    public String getOverdueTesensoInvoices() {
        return api.get("/api/tesenso/invoice/overdue");
    }

    @Tool(description = "Use this to mark a Tesenso invoice as paid. Confirm with the user before calling this.")
    public String markTesensoInvoicePaid(
            @ToolParam(description = "Invoice id. " + UUID_DESC) @NotBlank String invoiceId) {
        return api.post("/api/tesenso/invoice/" + invoiceId + "/paid", null);
    }

    @Tool(description = "Use this to get contracts of one customer, paginated.")
    public String getCustomerTesensoContracts(
            @ToolParam(description = "Customer id. " + UUID_DESC) @NotBlank String customerId,
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page) {
        return api.get("/api/tesenso/customer/" + customerId + "/contracts",
                pageParams(pageSize, page, null, null, null));
    }

}
