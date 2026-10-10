package com.turtledot.studio.payment;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.studio.Studio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    // Luxury & Corporate Slate Color Palette
    private static final Color COLOR_PRIMARY = new Color(15, 23, 42);       // #0F172A (Deep Slate 900)
    private static final Color COLOR_SECONDARY = new Color(30, 41, 59);     // #1E293B (Slate 800)
    private static final Color COLOR_AMBER = new Color(180, 83, 9);         // #B45309 (Amber 700)
    private static final Color COLOR_AMBER_LIGHT = new Color(254, 243, 199);// #FEF3C7 (Amber 100)
    private static final Color COLOR_AMBER_BORDER = new Color(245, 158, 11);// #F59E0B (Amber 500)
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);   // #F8FAFC (Slate 50)
    private static final Color COLOR_BG_HEADER = new Color(241, 245, 249);  // #F1F5F9 (Slate 100)
    private static final Color COLOR_BORDER = new Color(226, 232, 240);     // #E2E8F0 (Slate 200)
    private static final Color COLOR_BORDER_DARK = new Color(203, 213, 225);// #CBD5E1 (Slate 300)
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // #64748B (Slate 500)
    private static final Color COLOR_TEXT_BODY = new Color(51, 65, 85);     // #334155 (Slate 700)
    private static final Color COLOR_GREEN_TEXT = new Color(4, 120, 87);    // #047857 (Emerald 700)
    private static final Color COLOR_GREEN_BG = new Color(236, 253, 245);   // #ECFDF5 (Emerald 50)
    private static final Color COLOR_GREEN_BORDER = new Color(167, 243, 208);// #A7F3D0 (Emerald 200)

    public byte[] generatePaymentInvoicePdf(
            Payment payment,
            Studio studio,
            Client client,
            Event event,
            BigDecimal cumulativePaidSoFar
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 page with crisp, compact 26pt margins for clean single-page presentation
            Document document = new Document(PageSize.A4, 26, 26, 26, 26);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add top colored accent banner and running bottom footer
            writer.setPageEvent(new InvoicePageEventHelper(studio.getName()));
            document.open();

            // Font Palette
            Font fontStudioTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, COLOR_PRIMARY);
            Font fontBrandSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_AMBER);
            Font fontInvoiceHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, COLOR_PRIMARY);
            Font fontCardHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_PRIMARY);
            Font fontRegular = FontFactory.getFont(FontFactory.HELVETICA, 8f, COLOR_TEXT_BODY);
            Font fontRegularBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_PRIMARY);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_BODY);
            Font fontSmallBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_PRIMARY);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_MUTED);
            Font fontMutedSmall = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_TEXT_MUTED);
            Font fontTh = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, Color.WHITE);
            Font fontPaidPill = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_GREEN_TEXT);
            Font fontTotalLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);
            Font fontTotalBanner = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Color.WHITE);
            Font fontTotalCurrency = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.WHITE);

            String currency = (studio.getCurrency() != null && !studio.getCurrency().isBlank()) ? studio.getCurrency() : "INR";
            String currSymbol = "INR".equalsIgnoreCase(currency) ? "Rs.\u00A0" : ("USD".equalsIgnoreCase(currency) ? "$\u00A0" : currency + "\u00A0");

            // Invoice Number resolution
            String invoiceNo = payment.getInvoiceNumber();
            if (invoiceNo == null || invoiceNo.isBlank()) {
                int year = payment.getPaymentDate() != null ? payment.getPaymentDate().getYear() : LocalDate.now().getYear();
                long id = payment.getId() != null ? payment.getId() : 1L;
                invoiceNo = String.format("INV-%04d-%05d", year, id);
            }

            // GST Calculations
            BigDecimal paymentAmount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            BigDecimal gstRatePercent = studio.getGstRatePercent() != null ? studio.getGstRatePercent() : new BigDecimal("18.00");
            BigDecimal gstFactor = BigDecimal.ONE.add(gstRatePercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

            BigDecimal taxableAmount = paymentAmount.divide(gstFactor, 2, RoundingMode.HALF_UP);
            BigDecimal totalTax = paymentAmount.subtract(taxableAmount);
            BigDecimal halfGstRate = gstRatePercent.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal cgstAmount = totalTax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal sgstAmount = totalTax.subtract(cgstAmount);

            // Total Project & Balance
            BigDecimal agreedPackageAmount = client.getTotalAmount() != null ? client.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal totalPaid = cumulativePaidSoFar != null ? cumulativePaidSoFar : paymentAmount;
            BigDecimal balanceRemaining = agreedPackageAmount.subtract(totalPaid).max(BigDecimal.ZERO);

            // Studio Tax Credentials
            String gstin = (studio.getGstin() != null && !studio.getGstin().isBlank()) ? studio.getGstin() : "27AAPFU0939F1ZV";
            String pan = (studio.getPanNumber() != null && !studio.getPanNumber().isBlank()) ? studio.getPanNumber() : (gstin.length() >= 12 ? gstin.substring(2, 12) : "AAPFU0939F");
            String state = (studio.getStateName() != null && !studio.getStateName().isBlank()) ? studio.getStateName() : "Maharashtra";
            String stateCode = (studio.getStateCode() != null && !studio.getStateCode().isBlank()) ? studio.getStateCode() : "27";

            // Load Brand Logo Image (with fallback)
            Image logoImg = loadStudioLogo();

            // ==========================================
            // 1. TOP HEADER: BRAND IDENTITY & INVOICE META
            // ==========================================
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{58f, 42f});
            headerTable.setSpacingAfter(4f);

            // Left: Brand Identity Block
            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setPadding(0);

            PdfPTable brandInner = new PdfPTable(logoImg != null ? 2 : 1);
            brandInner.setWidthPercentage(100);
            if (logoImg != null) {
                brandInner.setWidths(new float[]{18f, 82f});

                PdfPCell logoCell = new PdfPCell(logoImg, true);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setPaddingRight(8f);
                brandInner.addCell(logoCell);
            }

            PdfPCell textBrandCell = new PdfPCell();
            textBrandCell.setBorder(Rectangle.NO_BORDER);
            textBrandCell.setPadding(0);

            Paragraph pStudioName = new Paragraph(studio.getName() != null ? studio.getName() : "Turtledot Studio", fontStudioTitle);
            textBrandCell.addElement(pStudioName);

            Paragraph pTagline = new Paragraph("PREMIUM PHOTOGRAPHY & CINEMATIC MEDIA PLATFORM", fontBrandSubtitle);
            pTagline.setSpacingAfter(2f);
            textBrandCell.addElement(pTagline);

            if (studio.getAddress() != null && !studio.getAddress().isBlank()) {
                Paragraph pAddr = new Paragraph(studio.getAddress(), fontSmall);
                textBrandCell.addElement(pAddr);
            }

            StringBuilder contactLine = new StringBuilder();
            if (studio.getEmail() != null && !studio.getEmail().isBlank()) {
                contactLine.append(studio.getEmail());
            }
            if (studio.getPhone() != null && !studio.getPhone().isBlank()) {
                if (contactLine.length() > 0) contactLine.append("  •  ");
                contactLine.append(studio.getPhone());
            }
            if (contactLine.length() > 0) {
                Paragraph pContact = new Paragraph(contactLine.toString(), fontSmall);
                textBrandCell.addElement(pContact);
            }

            brandInner.addCell(textBrandCell);
            brandCell.addElement(brandInner);

            // Studio Tax Registration Capsule (GSTIN / PAN / State)
            PdfPTable taxCapsule = new PdfPTable(3);
            taxCapsule.setWidthPercentage(100);
            taxCapsule.setWidths(new float[]{37f, 23f, 40f});
            taxCapsule.setSpacingBefore(4f);

            addCapsuleCell(taxCapsule, "GSTIN", gstin, fontMutedSmall, fontSmallBold);
            addCapsuleCell(taxCapsule, "PAN", pan, fontMutedSmall, fontSmallBold);
            addCapsuleCell(taxCapsule, "Place of Supply", state + " (" + stateCode + ")", fontMutedSmall, fontSmallBold);

            brandCell.addElement(taxCapsule);
            headerTable.addCell(brandCell);

            // Right: Tax Invoice Title & Metadata Table
            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setPadding(0);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pInvTitle = new Paragraph("TAX INVOICE", fontInvoiceHeader);
            pInvTitle.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(pInvTitle);

            Paragraph pInvSub = new Paragraph("ORIGINAL FOR RECIPIENT  •  GST PAYMENT RECEIPT", fontMutedSmall);
            pInvSub.setAlignment(Element.ALIGN_RIGHT);
            pInvSub.setSpacingAfter(4f);
            invoiceMetaCell.addElement(pInvSub);

            // Status Pill Badge
            PdfPTable statusTable = new PdfPTable(1);
            statusTable.setWidthPercentage(60);
            statusTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell statusCell = new PdfPCell(new Phrase("✔  PAYMENT SETTLED & VERIFIED", fontPaidPill));
            statusCell.setBackgroundColor(COLOR_GREEN_BG);
            statusCell.setBorderColor(COLOR_GREEN_BORDER);
            statusCell.setBorderWidth(0.75f);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setPaddingTop(2f);
            statusCell.setPaddingBottom(2.5f);
            statusTable.addCell(statusCell);
            statusTable.setSpacingAfter(4f);
            invoiceMetaCell.addElement(statusTable);

            // Metadata Grid Box
            PdfPTable metaGrid = new PdfPTable(2);
            metaGrid.setWidthPercentage(100);
            metaGrid.setWidths(new float[]{42f, 58f});

            addMetaGridRow(metaGrid, "Invoice Number:", invoiceNo, fontMuted, fontRegularBold, true);
            addMetaGridRow(metaGrid, "Invoice Date:", payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER), fontMuted, fontRegular, false);
            addMetaGridRow(metaGrid, "Payment Reference:", payment.getPaymentReference() != null ? payment.getPaymentReference() : "—", fontMuted, fontRegular, false);
            addMetaGridRow(metaGrid, "Payment Method:", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name().replace('_', ' ') : "DIRECT", fontMuted, fontRegular, false);

            invoiceMetaCell.addElement(metaGrid);
            headerTable.addCell(invoiceMetaCell);

            document.add(headerTable);

            // Thin hairline separator
            LineSeparator separator = new LineSeparator(0.5f, 100, COLOR_BORDER, Element.ALIGN_CENTER, -1);
            document.add(new Chunk(separator));
            document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 2)));

            // ==========================================
            // 2. BILLED TO (CLIENT) vs PROJECT OVERVIEW
            // ==========================================
            PdfPTable twoCardTable = new PdfPTable(2);
            twoCardTable.setWidthPercentage(100);
            twoCardTable.setWidths(new float[]{50f, 50f});
            twoCardTable.setSpacingBefore(3f);
            twoCardTable.setSpacingAfter(5f);

            // Card 1: Billed To (Client)
            PdfPCell clientCard = new PdfPCell();
            clientCard.setBackgroundColor(COLOR_BG_LIGHT);
            clientCard.setBorderColor(COLOR_BORDER);
            clientCard.setBorderWidth(0.75f);
            clientCard.setPadding(0);

            // Card Header Band
            PdfPTable clientCardHeader = createCardHeaderTable("BILLED TO  (CLIENT DETAILS)", fontCardHeader);
            clientCard.addElement(clientCardHeader);

            // Card Body
            PdfPTable clientBody = new PdfPTable(1);
            clientBody.setWidthPercentage(100);
            PdfPCell cBodyCell = new PdfPCell();
            cBodyCell.setBorder(Rectangle.NO_BORDER);
            cBodyCell.setPadding(6f);
            cBodyCell.setPaddingTop(4f);

            Paragraph pClientName = new Paragraph(client.getName(), fontRegularBold);
            cBodyCell.addElement(pClientName);

            if (client.getClientCode() != null && !client.getClientCode().isBlank()) {
                Paragraph pClientId = new Paragraph("Client Code: " + client.getClientCode(), fontMutedSmall);
                cBodyCell.addElement(pClientId);
            }

            StringBuilder clientContact = new StringBuilder();
            if (client.getPhone() != null && !client.getPhone().isBlank()) {
                clientContact.append("Ph: ").append(client.getPhone());
            }
            if (client.getEmail() != null && !client.getEmail().isBlank()) {
                if (clientContact.length() > 0) clientContact.append("  •  ");
                clientContact.append(client.getEmail());
            }
            if (clientContact.length() > 0) {
                cBodyCell.addElement(new Paragraph(clientContact.toString(), fontSmall));
            }

            StringBuilder clientAddr = new StringBuilder();
            if (client.getAddress() != null && !client.getAddress().isBlank()) clientAddr.append(client.getAddress());
            if (client.getCity() != null && !client.getCity().isBlank()) {
                if (clientAddr.length() > 0) clientAddr.append(", ");
                clientAddr.append(client.getCity());
            }
            if (client.getState() != null && !client.getState().isBlank()) {
                if (clientAddr.length() > 0) clientAddr.append(", ");
                clientAddr.append(client.getState());
            }
            if (clientAddr.length() > 0) {
                cBodyCell.addElement(new Paragraph(clientAddr.toString(), fontSmall));
            }

            String clientGstin = (client.getGstin() != null && !client.getGstin().isBlank()) ? client.getGstin() : "Unregistered (B2C Consumer)";
            Paragraph pClientGst = new Paragraph();
            pClientGst.add(new Chunk("GSTIN: ", fontSmallBold));
            pClientGst.add(new Chunk(clientGstin, fontSmall));
            cBodyCell.addElement(pClientGst);

            clientBody.addCell(cBodyCell);
            clientCard.addElement(clientBody);
            twoCardTable.addCell(clientCard);

            // Card 2: Event & Booking Details
            PdfPCell eventCard = new PdfPCell();
            eventCard.setBackgroundColor(COLOR_BG_LIGHT);
            eventCard.setBorderColor(COLOR_BORDER);
            eventCard.setBorderWidth(0.75f);
            eventCard.setPadding(0);

            // Card Header Band
            PdfPTable eventCardHeader = createCardHeaderTable("EVENT & SERVICE OVERVIEW", fontCardHeader);
            eventCard.addElement(eventCardHeader);

            // Card Body
            PdfPTable eventBody = new PdfPTable(1);
            eventBody.setWidthPercentage(100);
            PdfPCell eBodyCell = new PdfPCell();
            eBodyCell.setBorder(Rectangle.NO_BORDER);
            eBodyCell.setPadding(6f);
            eBodyCell.setPaddingTop(4f);

            if (event != null) {
                eBodyCell.addElement(new Paragraph(event.getEventName(), fontRegularBold));
                if (event.getEventType() != null && !event.getEventType().isBlank()) {
                    eBodyCell.addElement(new Paragraph("Service: " + event.getEventType() + " Media Coverage", fontSmall));
                }
                if (event.getEventDate() != null) {
                    eBodyCell.addElement(new Paragraph("Shoot Date: " + event.getEventDate().format(DATE_FORMATTER), fontSmall));
                }
                if (event.getLocation() != null && !event.getLocation().isBlank()) {
                    eBodyCell.addElement(new Paragraph("Location: " + event.getLocation(), fontSmall));
                }
            } else {
                eBodyCell.addElement(new Paragraph("Creative Photography & Cinematography Services", fontRegularBold));
                eBodyCell.addElement(new Paragraph("Category: Professional Digital Media Production", fontSmall));
            }

            // Financial milestone line
            Paragraph pPackageRow = new Paragraph();
            pPackageRow.add(new Chunk("Agreed Package: ", fontSmallBold));
            pPackageRow.add(new Chunk(currSymbol + CURRENCY_FORMAT.format(agreedPackageAmount) + "    ", fontSmall));
            pPackageRow.add(new Chunk("Balance: ", fontSmallBold));
            pPackageRow.add(new Chunk(currSymbol + CURRENCY_FORMAT.format(balanceRemaining), fontSmallBold));
            eBodyCell.addElement(pPackageRow);

            eventBody.addCell(eBodyCell);
            eventCard.addElement(eventBody);
            twoCardTable.addCell(eventCard);

            document.add(twoCardTable);

            // ==========================================
            // 3. GST ITEMIZED SERVICE TABLE
            // ==========================================
            PdfPTable itemsTable = new PdfPTable(7);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{5f, 37f, 10f, 12f, 12f, 12f, 12f});
            itemsTable.setSpacingBefore(2f);
            itemsTable.setSpacingAfter(5f);

            // Table Headers
            addTableHeader(itemsTable, "Sl.", Element.ALIGN_CENTER, fontTh);
            addTableHeader(itemsTable, "Description of Service & Particulars", Element.ALIGN_LEFT, fontTh);
            addTableHeader(itemsTable, "SAC Code", Element.ALIGN_CENTER, fontTh);
            addTableHeader(itemsTable, "Taxable Value (" + currency + ")", Element.ALIGN_RIGHT, fontTh);
            addTableHeader(itemsTable, "CGST (" + halfGstRate + "%)", Element.ALIGN_RIGHT, fontTh);
            addTableHeader(itemsTable, "SGST (" + halfGstRate + "%)", Element.ALIGN_RIGHT, fontTh);
            addTableHeader(itemsTable, "Total (" + currency + ")", Element.ALIGN_RIGHT, fontTh);

            // Item Row Description
            String serviceTitle = "Creative Photography, Video Production & Digital Media Delivery";
            if (event != null && event.getEventName() != null) {
                serviceTitle = event.getEventName() + " (" + (event.getEventType() != null ? event.getEventType() : "Media") + " Coverage)";
            }
            String noteLine = (payment.getNotes() != null && !payment.getNotes().isBlank()) ? payment.getNotes() : "Client payment installment";

            addTableRow(itemsTable, "1", Element.ALIGN_CENTER, fontRegular, COLOR_BG_LIGHT);

            // Description Cell with Title + Note
            PdfPCell descCell = new PdfPCell();
            descCell.setBackgroundColor(COLOR_BG_LIGHT);
            descCell.setBorderColor(COLOR_BORDER);
            descCell.setBorderWidth(0.5f);
            descCell.setPadding(5f);
            Paragraph pItemTitle = new Paragraph(serviceTitle, fontRegularBold);
            Paragraph pItemSub = new Paragraph("Milestone / Note: " + noteLine, fontMutedSmall);
            descCell.addElement(pItemTitle);
            descCell.addElement(pItemSub);
            itemsTable.addCell(descCell);

            addTableRow(itemsTable, "998381", Element.ALIGN_CENTER, fontRegularBold, COLOR_BG_LIGHT);
            addTableRow(itemsTable, CURRENCY_FORMAT.format(taxableAmount), Element.ALIGN_RIGHT, fontRegular, COLOR_BG_LIGHT);
            addTableRow(itemsTable, CURRENCY_FORMAT.format(cgstAmount), Element.ALIGN_RIGHT, fontRegular, COLOR_BG_LIGHT);
            addTableRow(itemsTable, CURRENCY_FORMAT.format(sgstAmount), Element.ALIGN_RIGHT, fontRegular, COLOR_BG_LIGHT);
            addTableRow(itemsTable, CURRENCY_FORMAT.format(paymentAmount), Element.ALIGN_RIGHT, fontRegularBold, COLOR_BG_LIGHT);

            document.add(itemsTable);

            // ==========================================
            // 4. TOTALS & SUMMARY SECTION (TWO-COLUMN)
            // ==========================================
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{53f, 47f});
            totalsTable.setSpacingBefore(2f);
            totalsTable.setSpacingAfter(6f);

            // Left Column: Amount in Words + Statutory Notes
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(0);
            leftCell.setPaddingRight(8f);

            // Amount in Words Box
            PdfPTable wordsBox = new PdfPTable(1);
            wordsBox.setWidthPercentage(100);
            PdfPCell innerWords = new PdfPCell();
            innerWords.setBackgroundColor(COLOR_AMBER_LIGHT);
            innerWords.setBorderColor(COLOR_AMBER_BORDER);
            innerWords.setBorderWidth(0.75f);
            innerWords.setPadding(6f);

            Paragraph pWordsLabel = new Paragraph("INVOICE AMOUNT IN WORDS", fontCardHeader);
            innerWords.addElement(pWordsLabel);

            String words = NumberToWordsConverter.convert(paymentAmount, currency);
            Paragraph pWordsValue = new Paragraph(words, fontRegularBold);
            pWordsValue.setSpacingBefore(2f);
            innerWords.addElement(pWordsValue);

            wordsBox.addCell(innerWords);
            leftCell.addElement(wordsBox);

            // Statutory Tax Note Box
            PdfPTable statNoteBox = new PdfPTable(1);
            statNoteBox.setWidthPercentage(100);
            statNoteBox.setSpacingBefore(4f);
            PdfPCell innerStat = new PdfPCell();
            innerStat.setBackgroundColor(COLOR_BG_LIGHT);
            innerStat.setBorderColor(COLOR_BORDER);
            innerStat.setBorderWidth(0.5f);
            innerStat.setPadding(5f);

            Paragraph pStatHeading = new Paragraph("STATUTORY GST DECLARATION", fontCardHeader);
            innerStat.addElement(pStatHeading);

            Paragraph pStatBody = new Paragraph(
                    "• Services classified under SAC 998381 (Photographic & Video Recording Services).\n" +
                    "• GST Reverse Charge (RCM): NOT APPLICABLE.\n" +
                    "• Input Tax Credit (ITC) is available to registered recipients as per Section 16 of CGST Act.\n" +
                    "• Electronic Tax Invoice issued under Section 31 of CGST Act, 2017.",
                    fontMutedSmall
            );
            pStatBody.setSpacingBefore(2f);
            innerStat.addElement(pStatBody);

            statNoteBox.addCell(innerStat);
            leftCell.addElement(statNoteBox);

            totalsTable.addCell(leftCell);

            // Right Column: Tax Breakdown & Total Highlight Box
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBackgroundColor(COLOR_BG_LIGHT);
            rightCell.setBorderColor(COLOR_BORDER);
            rightCell.setBorderWidth(0.75f);
            rightCell.setPadding(0);

            // Breakdown table inside
            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setWidths(new float[]{45f, 55f});

            addSummaryRow(breakdownTable, "Taxable Base Amount:", currSymbol + CURRENCY_FORMAT.format(taxableAmount), fontRegular, fontRegular);
            addSummaryRow(breakdownTable, "Central GST (CGST @ " + halfGstRate + "%):", currSymbol + CURRENCY_FORMAT.format(cgstAmount), fontRegular, fontRegular);
            addSummaryRow(breakdownTable, "State GST (SGST @ " + halfGstRate + "%):", currSymbol + CURRENCY_FORMAT.format(sgstAmount), fontRegular, fontRegular);
            addSummaryRow(breakdownTable, "Total GST (" + gstRatePercent + "%):", currSymbol + CURRENCY_FORMAT.format(totalTax), fontRegular, fontRegular);

            // Highlighted Total Row (Deep Navy Banner)
            PdfPCell totalLblCell = new PdfPCell(new Phrase("TOTAL PAID (INCL. GST):", fontTotalLabel));
            totalLblCell.setBackgroundColor(COLOR_PRIMARY);
            totalLblCell.setBorder(Rectangle.NO_BORDER);
            totalLblCell.setPadding(5f);
            totalLblCell.setPaddingLeft(6f);
            breakdownTable.addCell(totalLblCell);

            Phrase totalPhrase = new Phrase();
            totalPhrase.add(new Chunk(currSymbol.trim() + "\u00A0", fontTotalCurrency));
            totalPhrase.add(new Chunk(CURRENCY_FORMAT.format(paymentAmount), fontTotalBanner));

            PdfPCell totalValCell = new PdfPCell(totalPhrase);
            totalValCell.setBackgroundColor(COLOR_PRIMARY);
            totalValCell.setBorder(Rectangle.NO_BORDER);
            totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValCell.setPadding(5f);
            totalValCell.setPaddingRight(6f);
            totalValCell.setNoWrap(true);
            breakdownTable.addCell(totalValCell);

            // Contract Progress Line
            addSummaryRow(breakdownTable, "Cumulative Paid to Date:", currSymbol + CURRENCY_FORMAT.format(totalPaid), fontMutedSmall, fontSmallBold);
            addSummaryRow(breakdownTable, "Remaining Balance Due:", currSymbol + CURRENCY_FORMAT.format(balanceRemaining), fontMutedSmall, fontSmallBold);

            rightCell.addElement(breakdownTable);
            totalsTable.addCell(rightCell);

            document.add(totalsTable);

            // ==========================================
            // 5. TERMS & DIGITAL VERIFICATION SIGNATURE
            // ==========================================
            PdfPTable authTable = new PdfPTable(2);
            authTable.setWidthPercentage(100);
            authTable.setWidths(new float[]{62f, 38f});
            authTable.setSpacingBefore(2f);

            // Terms & Conditions (Left)
            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(Rectangle.NO_BORDER);
            termsCell.setPadding(0);
            termsCell.setPaddingRight(10f);

            Paragraph pTermsTitle = new Paragraph("TERMS & CONDITIONS", fontCardHeader);
            termsCell.addElement(pTermsTitle);

            Paragraph pTerms = new Paragraph(
                    "1. We declare that this invoice shows the actual price of services described and all particulars are true and correct.\n" +
                    "2. This electronic tax invoice receipt is computer generated and legally verified. No physical signature is required.\n" +
                    "3. Deliverables, copyright licenses, and delivery milestones are governed by the master studio agreement.",
                    fontMutedSmall
            );
            pTerms.setSpacingBefore(2f);
            termsCell.addElement(pTerms);
            authTable.addCell(termsCell);

            // Digital Stamp & Verification Box (Right)
            PdfPCell signCell = new PdfPCell();
            signCell.setBackgroundColor(COLOR_GREEN_BG);
            signCell.setBorderColor(COLOR_GREEN_BORDER);
            signCell.setBorderWidth(1f);
            signCell.setPadding(6f);
            signCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pSignFor = new Paragraph("For " + (studio.getName() != null ? studio.getName() : "Turtledot Studio"), fontRegularBold);
            pSignFor.setAlignment(Element.ALIGN_CENTER);
            signCell.addElement(pSignFor);

            Paragraph pStamp = new Paragraph("✔  DIGITALLY VERIFIED DOCUMENT", fontPaidPill);
            pStamp.setAlignment(Element.ALIGN_CENTER);
            pStamp.setSpacingBefore(2f);
            signCell.addElement(pStamp);

            Paragraph pStampMeta = new Paragraph(
                    "System Generated • " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\nAuthorized Finance & Accounts Desk",
                    fontMutedSmall
            );
            pStampMeta.setAlignment(Element.ALIGN_CENTER);
            pStampMeta.setSpacingBefore(1f);
            signCell.addElement(pStampMeta);

            authTable.addCell(signCell);
            document.add(authTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate payment invoice PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating payment invoice PDF: " + e.getMessage(), e);
        }
    }

    private Image loadStudioLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/logo.png");
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                Image img = Image.getInstance(bytes);
                img.scaleToFit(44f, 44f);
                return img;
            }
            // Fallback filesystem check
            File file = new File("frontend/public/turtledot_app_icon_color.png");
            if (file.exists()) {
                Image img = Image.getInstance(file.getAbsolutePath());
                img.scaleToFit(44f, 44f);
                return img;
            }
        } catch (Exception e) {
            log.debug("Logo could not be loaded into PDF: {}", e.getMessage());
        }
        return null;
    }

    private PdfPTable createCardHeaderTable(String title, Font font) {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(COLOR_BG_HEADER);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.75f);
        cell.setPadding(4f);
        cell.setPaddingLeft(6f);
        header.addCell(cell);
        return header;
    }

    private void addCapsuleCell(PdfPTable table, String label, String val, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(2.5f);
        cell.setPaddingLeft(4f);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(val, valFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addMetaGridRow(PdfPTable table, String label, String value, Font labelFont, Font valFont, boolean isHighlight) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setPadding(1.5f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(1.5f);
        table.addCell(c2);
    }

    private void addTableHeader(PdfPTable table, String header, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(header, font));
        cell.setBackgroundColor(COLOR_SECONDARY);
        cell.setBorderColor(COLOR_SECONDARY);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String text, int align, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String val, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(COLOR_BORDER);
        c1.setBorderWidth(0.5f);
        c1.setPadding(3.5f);
        c1.setPaddingLeft(6f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val, valFont));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(COLOR_BORDER);
        c2.setBorderWidth(0.5f);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(3.5f);
        c2.setPaddingRight(6f);
        c2.setNoWrap(true);
        table.addCell(c2);
    }

    /**
     * Page Event Helper to draw high-end accent bars at the top of the page
     * and clean page number & powered-by footer at the bottom.
     */
    private static class InvoicePageEventHelper extends PdfPageEventHelper {
        private final String studioName;

        public InvoicePageEventHelper(String studioName) {
            this.studioName = (studioName != null && !studioName.isBlank()) ? studioName : "Turtledot Studio";
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageWidth = document.getPageSize().getWidth();
            float pageHeight = document.getPageSize().getHeight();

            // 1. Top Decorative Brand Stripes
            // Slate stripe (4pt)
            cb.setColorFill(COLOR_PRIMARY);
            cb.rectangle(0, pageHeight - 4, pageWidth, 4);
            cb.fill();

            // Amber accent stripe (2.5pt)
            cb.setColorFill(COLOR_AMBER_BORDER);
            cb.rectangle(0, pageHeight - 6.5f, pageWidth, 2.5f);
            cb.fill();

            // 2. Bottom Running Footer Separator Line
            cb.setColorStroke(COLOR_BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(26, 20);
            cb.lineTo(pageWidth - 26, 20);
            cb.stroke();

            // 3. Bottom Running Footer Text
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_TEXT_MUTED);
            Phrase footerLeft = new Phrase("Official Electronic GST Tax Invoice  •  " + studioName, fontFooter);
            Phrase footerRight = new Phrase("Page 1 of 1  •  Powered by Turtledot Studio CRM", fontFooter);

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, footerLeft, 26, 11, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footerRight, pageWidth - 26, 11, 0);
        }
    }
}
