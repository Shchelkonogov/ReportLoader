package ru.tecon.parser.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class ParameterData implements Serializable {
	
	private String name;
	private List<String> data = new ArrayList<>();
	
	public ParameterData(String name) {
		this.name = name;
	}
	
	public List<String> getData() {
		return data;
	}

	public void setData(List<String> data) {
		this.data = data;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	void sort() {
		data.sort((o1, o2) -> {
			if (o1.equals("") || o2.equals("")) {
				return 0;
			}
			try {
				BigDecimal bigDecimal1 = new BigDecimal(o1);
				BigDecimal bigDecimal2 = new BigDecimal(o2);
				return bigDecimal1.compareTo(bigDecimal2);
			} catch (NumberFormatException e) {
				LocalDate localDate1 = LocalDate.parse(o1, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
				LocalDate localDate2 = LocalDate.parse(o2, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
				return localDate1.compareTo(localDate2);
			}
		});
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ParameterData.class.getSimpleName() + "[", "]")
				.add("name='" + name + "'")
				.add("data=" + data)
				.toString();
	}
}