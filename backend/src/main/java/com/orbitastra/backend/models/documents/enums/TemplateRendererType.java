package com.orbitastra.backend.models.documents.enums;

/**
 * How a template is turned into a finished document.
 *
 * <p>Kept on the template because the school chooses this once, per template, and
 * the code making the file has to know which way to do it.
 */
public enum TemplateRendererType {
    /** Marked-up text with placeholders, turned into a PDF. */
    HTML,

    /** An uploaded Word file with placeholders in it. */
    DOCX,

    /** An uploaded PDF with form fields that get filled in. */
    PDF_FORM
}
