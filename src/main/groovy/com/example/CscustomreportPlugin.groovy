/*
* Copyright 2022 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example

import com.morpheusdata.core.Plugin

class CscustomreportPlugin extends Plugin {

    @Override
    String getCode() {
        return 'cscr'
    }

    @Override
    void initialize() {
        this.setName("CS VM Financials")
        this.registerProvider(new CscustomreportReportProvider(this,this.morpheus))
        this.setAuthor("Anish Abraham")
    }

    public goodDate(String current, Boolean add) {
        // we format the date to something we can use in the Sql
        // we can optionally also bump the month/year to make inclusive(for end date)
        String[] splt
        splt = current.split("/")

        Integer year = Integer.valueOf(splt[1])
        Integer month = Integer.valueOf(splt[0])

        String stYear
        String stMonth

        if (add) {
            if (month < 12) {
                month += 1
                if (month < 10) {
                    stMonth = "0${month}"
                } else {
                    stMonth = "${month}"
                }
            } else {
                stMonth = "01"
                year ++
            }

            stYear = "${year}"
        } else {
            stYear = splt[1]
            stMonth = splt[0]
        }

        return stYear + "-" + stMonth
    }

    /**
     * Called when a plugin is being removed from the plugin manager (aka Uninstalled)
     */
    @Override
    void onDestroy() {
        //nothing to do for now
    }
}
