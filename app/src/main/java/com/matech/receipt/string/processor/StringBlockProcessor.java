package com.matech.receipt.string.processor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

/**
 * Created by gango on 2018-02-02.
 */

public class StringBlockProcessor {
    private ArrayList<TextBlock> productBlocks = new ArrayList<>();
    private ArrayList<TextBlock> priceBlocks = new ArrayList<>();

    private Comparator<Text> blockHorizontalPositionCmp = new Comparator<Text>() {
        @Override
        public int compare(Text a, Text b) {
            return Integer.compare(a.getBoundingBox().top, b.getBoundingBox().top);
        }
    };

    public void clear() {
        productBlocks.clear();
        priceBlocks.clear();
    }

    public void addProductBlock(TextBlock block) {
        productBlocks.add(block);
    }

    public void addPriceBlock(TextBlock block) {
        priceBlocks.add(block);
    }

    public void processData(Context applicationContext, Activity ocrCaptureActivity) {
        ArrayList<Pair<Text, Text>> productesPrices = pairProductAndPrice();

        int n_lines = productesPrices.size();
        StringBuilder result = new StringBuilder();
        Pattern idPattern = Pattern.compile("^\\d{4,}\\s*");
        Pattern pricePattern = Pattern.compile("(\\d+)[,.]?(\\d*)");
        for (Pair<Text, Text> productePrice : productesPrices) {
            String name = productePrice.first.getValue();
            Matcher idMatcher = idPattern.matcher(name);
            name = idMatcher.replaceFirst("");
            String price = productePrice.second.getValue();
            Matcher priceMatcher = pricePattern.matcher(price);
            if (priceMatcher.find()) {
                String formated_price = priceMatcher.group(1) + "." + priceMatcher.group(2);
                result.append(name + ";" + formated_price + "\n");
            } else {
                Log.d(TAG, "No match :(");
            }
        }
        Intent shareIntent = ShareCompat.IntentBuilder.from(ocrCaptureActivity)
                .setType("text/plain")
                .setText(result.toString())
                .getIntent();
        if (shareIntent.resolveActivity(ocrCaptureActivity.getPackageManager()) != null) {
            ocrCaptureActivity.startActivity(shareIntent);
        }
        clear();
//        for(TextBlock block : productBlocks){
//            Point[] ps = block.getCornerPoints();
//            for(Point p : ps) {
//                Log.d(TAG, p.toString());
//            }
//        }
    }

    private ArrayList<Pair<Text, Text>> pairProductAndPrice() {
        ArrayList<Text> productLinesList = blocksToLines(productBlocks);
        Text[] productLines = listToArray(productLinesList);
        ArrayList<Text> priceLinesList = blocksToLines(priceBlocks);
        removeNonPrices(priceLinesList);
        Text[] priceLines = listToArray(priceLinesList);
        int reduce = 1;
        if (priceLines[0].getBoundingBox().top > productLines[0].getBoundingBox().top) {
            reduce = 2;
        }
        ArrayList<Pair<Text, Text>> productesPrices = new ArrayList<>();
        for (Text price : priceLines) {
            int i = Math.max(Math.abs(Arrays.binarySearch(productLines, price, blockHorizontalPositionCmp)) - reduce, 0);
            Text product = productLines[i];
            Pair<Text, Text> productePrice = new Pair<>(product, price);
            productesPrices.add(productePrice);
        }
        return productesPrices;
    }

    private void removeNonPrices(ArrayList<Text> priceLinesList) {
        ArrayList<Text> toRemove = new ArrayList<>();
        for (Text block : priceLinesList) {
            if (!isMostlyDigit(block)) {
                toRemove.add(block);
            }
        }
        priceLinesList.removeAll(toRemove);
    }

    @NonNull
    private Text[] listToArray(ArrayList<Text> productLinesList) {
        return productLinesList.toArray(new Text[productLinesList.size()]);
    }

    private boolean isMostlyDigit(Text price) {
        int nDigits = 0;
        String str = price.getValue();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                nDigits++;
            }
        }
        return nDigits * 3 > str.length();
    }

    private ArrayList<Text> blocksToLines(ArrayList<TextBlock> blocks) {
        Collections.sort(blocks, blockHorizontalPositionCmp);
        ArrayList<Text> lines = new ArrayList<>();
        for (TextBlock block : blocks) {
            lines.addAll(block.getComponents());
        }
        return lines;
    }
}
