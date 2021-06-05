package health.ere.ps.service.muster16.parser.rgxer.delegate;

import health.ere.ps.model.muster16.MedicationString;
import health.ere.ps.service.muster16.parser.rgxer.matcher.MedicationMatcher;
import health.ere.ps.service.muster16.parser.rgxer.model.MedicationRecord;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MedicationParser {

    private final MedicationDataIntermediateParser intermediateParser;
    private final MedicationMatcher matcher;

    private final int PZN_LENGTH = 8;
    private final Pattern PZN_PAT = Pattern.compile("(PZN)?\\s*:?\\s*(?<value>\\d{8})");
    final Pattern SIZE_PAT = Pattern.compile("\\b(N[1-3]|KP)\\b");

    public MedicationParser() {
        this.intermediateParser = new MedicationDataIntermediateParser();
        this.matcher = new MedicationMatcher();
    }

    public List<MedicationString> parse(String entry) {
        List<String> lines = intermediateParser.parse(entry);
        return lines.stream().map(this::parseLine).collect(Collectors.toList());
    }

    private MedicationString parseLine(String line) {
        String pzn = getPZN(line);
        String form = pzn != null ? getForm(pzn) : null;
        String size = getSize(line, pzn);
        String dosage = null;

        return new MedicationString(line, size, form, dosage, null, pzn);
    }

    private String getForm(String pzn) {
        MedicationRecord record = findRecord(pzn);
        return record != null ? record.getForm() : null;
    }

    private MedicationRecord findRecord(String pzn) {
        return matcher.findByPZN(pzn);
    }

    private String getSize(String entry, String pzn) {
        if (pzn != null) {
            MedicationRecord record = matcher.findByPZN(pzn);
            if (record != null)
                return record.getNorm();
        }
        return parseSize(entry);
    }

    private String parseSize(String entry) {
        Matcher matcher = SIZE_PAT.matcher(entry);
        return matcher.find() ? matcher.group() : null;
    }

    private String parseDosage(String entry) {
        // TODO implement method
        throw new UnsupportedOperationException();
    }


    private String getPZN(String entry) {
        String pzn;
        if ((pzn = extractPZN(entry)) != null) {
            return normalizePZN(pzn);
        } else {
            MedicationRecord record = matcher.bestMatch(entry);
            return record != null ? record.getPZN() : null;
        }
    }

    private String normalizePZN(String pzn) {
        final int paddingSize = PZN_LENGTH - pzn.length();
        final String padding = new String(new char[paddingSize]).replace("\0", "0");
        return padding + pzn;
    }

    private String extractPZN(String entry) {
        Matcher matcher = PZN_PAT.matcher(entry);
        return matcher.find() ? matcher.group("value") : null;
    }
}
