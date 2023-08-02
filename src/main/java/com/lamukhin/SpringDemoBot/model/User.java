package com.lamukhin.SpringDemoBot.model;


import java.util.Date;
import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name="usersDataTable")
@ToString
public class User {
	@Id
	@Getter @Setter
	private long chatId;
	@Getter @Setter
	private String firstName;
	@Getter @Setter
	private String lastName;
	@Getter @Setter
	private String userName;
	@Getter @Setter
	private Timestamp registeredAt;
	
}
