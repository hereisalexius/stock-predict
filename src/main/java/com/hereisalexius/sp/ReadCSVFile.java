/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hereisalexius.sp;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author kotik
 */
public class ReadCSVFile {
    
     private static final ReadCSVFile instance = new ReadCSVFile();
     private ReadCSVFile () {}
 
  public static ReadCSVFile getInstance() {
    return instance;
  }

    synchronized public List<Double> readCsvFile(String fileName) {
        FileReader fileReader = null;
        CSVParser csvFileParser = null;
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withSkipHeaderRecord(true);
          List<Double> entities = new ArrayList<>();


        try {

            //Create a new list of student to be filled by CSV file data 
          
            //initialize FileReader object
            fileReader = new FileReader(fileName);

            //initialize CSVParser object
            csvFileParser = new CSVParser(fileReader, csvFileFormat);

            //Get a list of CSV file records
            List csvRecords = csvFileParser.getRecords();

            //Read the CSV file records starting from the second record to skip the header
            for (int i = 1; i < csvRecords.size(); i++) {

                CSVRecord record = (CSVRecord) csvRecords.get(i);

                entities.add(Double.valueOf(record.get(6).replaceAll(",", ".")));
            }

            //Print the new student list
            for (Object stockse : entities) {

                System.out.println(stockse.toString());

            }

        } catch (Exception e) {

            System.out.println("Error in CsvFileReader !!!");

            e.printStackTrace();

        } finally {

            try {

                fileReader.close();

                csvFileParser.close();

            } catch (IOException e) {

                System.out.println("Error while closing fileReader/csvFileParser !!!");

                e.printStackTrace();

            }

        }
        return entities;
    }

   
}
