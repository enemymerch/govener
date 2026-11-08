package com.govener.finance.application;

public record FinanceRepositories(
    FinancePorts.Charges charges,
    FinancePorts.Payments payments,
    FinancePorts.Accounts accounts,
    FinancePorts.UnitAccounts unitAccounts,
    FinancePorts.Entries entries,
    FinancePorts.Expenses expenses,
    FinancePorts.ExpensePayments expensePayments,
    FinancePorts.Notifications notifications,
    FinancePorts.Batches batches) {}
