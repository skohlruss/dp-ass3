import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;
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
	public static void main(String[] args){
		File articlesFile = new File("data/articles.xml");
		File populationFile = new File("data/population.xml");
		
		List<Record> articles = parse(articlesFile);
		List<Record> populations = parse(populationFile);
		
		List<Data> data = combine(articles, populations);

		Map<String, String> result = calculate(data);
		
		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(result);
		System.out.println(json);
	}

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
}
