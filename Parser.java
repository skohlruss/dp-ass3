import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.text.DecimalFormat;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class Parser {
	private static class Record {
		public String year;
		public String country;
		public Double value;
		
		public String toString(){
			return String.format("Year %s, Country %s, Value %s\n", year, country, value);
		}
	}
	
	private static class Data {
		public String year;
		public String country;
		public Double population;
		public Double articles;
	
		public String toString(){
			return String.format("Year %s, Country %s, Population %s, Articles %s\n", year, country, population, articles);
		}
	}
	private static List<Record> parse(File file){	
		List<Record> records = new ArrayList<>();
	
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			NodeList recordNodes = doc.getElementsByTagName("record");
			for (int i = 0; i < recordNodes.getLength(); i++) {
				Node n = recordNodes.item(i);
	            if (n.getNodeType() == Node.ELEMENT_NODE) {
	               Element r = (Element) n;
	               Record rec = new Record();
	               NodeList fields = r.getElementsByTagName("field");
	               for(int j = 0; j < fields.getLength(); j++){
	            	   Node o = fields.item(j);
	            	   Element f = (Element) o;
	            	   String name = f.getAttribute("name");
	            	   switch (name){
		            	   case "Country or Area":
		            		   rec.country = f.getTextContent();
		            		   break;
		            	   case "Year":
		            		   rec.year = f.getTextContent();
		            		   break;
		            	   case "Value":
		            		   String val = f.getTextContent();
		            		   rec.value = val != null && val != "" ? Double.parseDouble(val) : null;
		            		   break;
	            	   }
	               }
	               records.add(rec);
	            }
	        }
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.out.println("ERROR: "+e.getMessage());
		}
		return records;
	}
	
	private static List<Data> combine(List<Record> articles, List<Record> populations){
		List<Data> data = new ArrayList<>();
		for(Record p : populations){
			for(Record a : articles){
				if(a.country.equals(p.country) && a.year.equals(p.year)){
					Data d = new Data();
					d.country = a.country;
					d.year = a.year;
					
					d.articles = a.value;
					d.population = p.value;
					data.add(d);
					break;
				}
			}
		}
		return data;
	}
	
	private static Map<String, String> calculate(List<Data> data){
		Map<String, Double> population = new HashMap<>();
		Map<String, Double> articles = new HashMap<>();

		for(Data d : data){
			if(d.year != null && d.country != null && d.articles != null && d.population != null){
				if(population.get(d.year) == null) population.put(d.year, 0.0);
				population.put(d.year, population.get(d.year) + d.population);
				
				if(articles.get(d.year) == null) articles.put(d.year, 0.0);
				articles.put(d.year, articles.get(d.year) + d.articles);
			}
		}
		
		Map<String, String> result = new TreeMap<>();
		for(Entry<String, Double> e : population.entrySet()){
			Double res = articles.get(e.getKey()) / e.getValue();
			DecimalFormat df = new DecimalFormat("#.###############");
			result.put(e.getKey(), df.format(res));
		}
		return result;
	}
	
	private static Map<String, String> cumulate(List<Record> records){
		Map<String, Double> tmp = new TreeMap<>();
		
		for(Record r : records){
			if(r.year != null && r.value != null){
				if(tmp.get(r.year) == null) tmp.put(r.year, 0.0);
				tmp.put(r.year, tmp.get(r.year)+r.value);
			}
		}
		
		Map<String, String> result = new TreeMap<>();
		for(Entry<String, Double> e : tmp.entrySet()){
			DecimalFormat df = new DecimalFormat("#.###############");
			result.put(e.getKey(), df.format(e.getValue()));
		}
		
		return result;
	}
	
	private static void insertDbRaw(List<Data> data){
		try { 
			Class.forName("org.mariadb.jdbc.Driver");
			Connection con = DriverManager.getConnection("jdbc:mariadb://localhost/experiment");
			con.setAutoCommit(false);
			
			PreparedStatement insert = con.prepareStatement("INSERT INTO raw (year, country, articles, population) VALUES (?, ?, ?, ?)");
			for(Data d : data){
				insert.setString(1, d.year);
				insert.setString(2,  d.country);
				if(d.articles != null) insert.setDouble(3, d.articles); else insert.setNull(3, Types.DOUBLE); 
				if(d.population != null) insert.setDouble(4, d.population); else insert.setNull(4, Types.DOUBLE); 
				insert.addBatch();
			}
			 
			insert.executeBatch();
			con.commit();
		} catch(Exception e){
			System.out.println("[ERROR] Could not insert raw data into DB: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static void insertDbResult(Map<String, String> result){
		try { 
			Class.forName("org.mariadb.jdbc.Driver");
			Connection con = DriverManager.getConnection("jdbc:mariadb://localhost/experiment");
			con.setAutoCommit(false);
			
			PreparedStatement insert = con.prepareStatement("INSERT INTO result (year, articles_per_capita) VALUES (?, ?)");
			for(Entry<String, String> e : result.entrySet()){
				insert.setString(1, e.getKey());
				insert.setString(2, e.getValue());
				insert.addBatch();
			}
			 
			insert.executeBatch();
			con.commit();
		} catch(Exception e){
			System.out.println("[ERROR] Could not insert result into DB: "+e.getMessage());

			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		File articlesFile = new File("data/articles.xml");
		File populationFile = new File("data/population.xml");
		
		//parse files
		List<Record> articles = parse(articlesFile);
		List<Record> populations = parse(populationFile);

		//combine raw data
		List<Data> data = combine(articles, populations);
		
		//insert into database
		insertDbRaw(data);

		//experiment
		Map<String, String> result = calculate(data);
		
		//insert into database
		insertDbResult(result);
		
		//output
		//	result
		Gson gson = new GsonBuilder().create();
		String resultJson = gson.toJson(result);
		//	raw articles
		gson = new GsonBuilder().create();
		String articlesJson= gson.toJson(cumulate(articles));
		//	raw population
		gson = new GsonBuilder().create();
		String populationJson= gson.toJson(cumulate(populations));
		//	create result array
		String outputJson = String.format("{\"raw\": {\"articles\": %s, \"population\": %s}, \"result\": %s}", articlesJson, populationJson, resultJson);
		System.out.println(outputJson);
	}
}
