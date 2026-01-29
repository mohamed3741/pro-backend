package com.sallahli.model.Enum;

/**
 * Workflow type for category lead distribution strategy.
 * 
 * LEAD_OFFER: Traditional lead auction model - leads are offered to multiple
 * pros
 * who can accept/reject within a time window.
 * 
 * FIRST_CLICK: Uber-style model - first pro to accept gets the lead
 * immediately,
 * no auction or waiting period.
 */
public enum WorkflowType {
    LEAD_OFFER,
    FIRST_CLICK
}
