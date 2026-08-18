package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

/** Exact Unicode code-point ordering, independent of UTF-16 code-unit ordering. */
final class UnicodeCodePointOrder {
    private UnicodeCodePointOrder() {
    }

    static int compare(String left, String right) {
        int leftOffset = 0;
        int rightOffset = 0;
        while (leftOffset < left.length() && rightOffset < right.length()) {
            int leftPoint = left.codePointAt(leftOffset);
            int rightPoint = right.codePointAt(rightOffset);
            int comparison = Integer.compare(leftPoint, rightPoint);
            if (comparison != 0) return comparison;
            leftOffset += Character.charCount(leftPoint);
            rightOffset += Character.charCount(rightPoint);
        }
        return Integer.compare(left.length() - leftOffset, right.length() - rightOffset);
    }
}
