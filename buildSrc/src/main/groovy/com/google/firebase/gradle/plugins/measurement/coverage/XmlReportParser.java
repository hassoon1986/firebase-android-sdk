// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.gradle.plugins.measurement.coverage;

import static com.google.firebase.gradle.plugins.measurement.MetricsServiceApi.Result;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Helper class that extracts coverage numbers from JaCoCo Xml report. */
public class XmlReportParser {

    private final String sdk;
    private final Document document;

    public XmlReportParser(String sdk, File report) {
        this.sdk = sdk;
        try {
            this.document = new SAXReader().read(report);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of {@link Result} containing the line coverage number for a SDK overall and
     * individual files in that SDK.
     */
    public List<Result> parse() {
        List<Result> results = new ArrayList<>();

        Node report = this.document.selectSingleNode("/report");
        double sdkCoverage = calculateCoverage(report);
        results.add(new Result(this.sdk, "", sdkCoverage));

        List<Node> sources = this.document.selectNodes("//sourcefile");
        for (Node source : sources) {
            String filename = source.valueOf("@name");
            double fileCoverage = calculateCoverage(source);
            results.add(new Result(this.sdk, filename, fileCoverage));
        }

        return results;
    }

    private double calculateCoverage(Node node) {
        Node counter = node.selectSingleNode("counter[@type='LINE']");
        if (counter != null) {
            int covered = Integer.parseInt(counter.valueOf("@covered"));
            int missed = Integer.parseInt(counter.valueOf("@missed"));
            return (double) covered / (covered + missed);
        }
        return 0;
    }
}
