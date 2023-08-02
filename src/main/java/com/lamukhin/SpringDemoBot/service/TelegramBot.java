package com.lamukhin.SpringDemoBot.service;

import java.util.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.lamukhin.SpringDemoBot.config.BotConfig;
import com.lamukhin.SpringDemoBot.model.User;
import com.lamukhin.SpringDemoBot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

	final BotConfig config;
	@Autowired
	private UserRepository userRepository;
	static final String HELP_TEXT = "This bot is created to demonstrate Spring Boot capabilities.\n\n"
			+ "You can execute commands from the main menu on the left or by typing a command: \n\n"
			+ "Type /start to see a welcome message \n\n" + "Type /my_data to see data stored about yourself\n\n"
			+ "Type /delete_data to delete your data from bot memory\n\n" + "Type /help to see this message again\n\n"
			+ "Type /settings to configurate this bot\n\n";
	static final String DELETE_DATA = "Are you sure you wanna delete your data from this bot?\n\n";
	static final String REMOVE_KEYBOARD = "Done!\n\n" + "If you want to try it again, enter /show_keyboard_menu\n\n";
	static final String SHOW_KEYBOARD = "Done!\n\n" + "Try to use Keyboard's buttons.\n\n";
	static final String YES_BUTTON = "yes";
	static final String NO_BUTTON = "no";

	public TelegramBot(BotConfig botConfig) {
		this.config = botConfig;
		List<BotCommand> listOfCommands = new ArrayList<>();
		listOfCommands.add(new BotCommand("/start", "get welcome message"));
		listOfCommands.add(new BotCommand("/my_data", "get your data"));
		listOfCommands.add(new BotCommand("/delete_data", "delete my data"));
		listOfCommands.add(new BotCommand("/help", "info about using this bot"));
		listOfCommands.add(new BotCommand("/show_keyboard_menu", "shows keyboard"));
		try {
			this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
		} catch (TelegramApiException ex) {
			log.error("Error setting bot's command list : " + ex.getMessage());
		} 

	}

	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();
			String userFirstName = update.getMessage().getChat().getFirstName();
			switch (messageText) {
			case "/start": {
				registerUser(update.getMessage());
				startCommandReceived(chatId, userFirstName);
				break;
			}
			case "/my_data": {
				postUserData(chatId);
				break;
			}
			case "/delete_data": {
				sendMessage(chatId, DELETE_DATA);
				break;
			}
			case "/help": {
				sendMessage(chatId, HELP_TEXT);
				break;
			}
			case "/show_keyboard_menu": {
				sendMessage(chatId, SHOW_KEYBOARD);
				break;
			}
			case "/hide_keyboard_menu": {
				String temporaryAnswer = EmojiParser.parseToUnicode(
						"Sorry, this function is not working yet. Please, hide keyboard manually :worried:");
				sendMessage(chatId,  temporaryAnswer/*REMOVE_KEYBOARD*/);
				break;
			}
			default:
				sendMessage(chatId, "Sorry, command was not recognized.");
			}
		} else if(update.hasCallbackQuery()) {
			String callbackData = update.getCallbackQuery().getData();
			long chatId = update.getCallbackQuery().getMessage().getChatId();
			if (callbackData.equals(YES_BUTTON)){
				deleteUserData(chatId);
			} else if (callbackData.equals(NO_BUTTON)) {
				sendMessage(chatId, "Ok, your data is still stored in DB. ");
			}
		}

	}

	private void deleteUserData(long chatId) {
		if (!(userRepository.findById(chatId).isEmpty())) {
			User user = userRepository.findById(chatId).get();
			log.info("User deleted: " + user);
			userRepository.delete(user);
			sendMessage(chatId, "Done!");
		} else {
			String notFoundUserMessage = "Sorry, there is no any info about you."
					+ "\nIf you want to register in this bot's chat, press /start";
			sendMessage(chatId, notFoundUserMessage);
		}
	}

	private void postUserData(long chatId) {
		if (!(userRepository.findById(chatId).isEmpty())) {
			User user = userRepository.findById(chatId).get();
			String userInfo = "Your chat ID: " + String.valueOf(user.getChatId()) + "\nYour First name: "
					+ user.getFirstName() + "\nYour Last name: " + user.getLastName() + "\nYour User name: "
					+ user.getUserName() + "\nYou registred in this bot's chat: " + user.getRegisteredAt().toString();
			sendMessage(chatId, userInfo);
		} else {
			String notFoundUserMessage = "Sorry, there is no any info about you."
					+ "\nIf you want to register in this bot's chat, press /start";
			sendMessage(chatId, notFoundUserMessage);
		}

	}

	private void registerUser(Message msg) {
		if (userRepository.findById(msg.getChatId()).isEmpty()) {
			var chatId = msg.getChatId();
			var chat = msg.getChat();
			User user = new User();
			user.setChatId(chatId);
			user.setFirstName(chat.getFirstName());
			user.setLastName(chat.getLastName());
			user.setUserName(chat.getUserName());
			user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
			userRepository.save(user);
			log.info("User saved: " + user);
		}

	}

	private void startCommandReceived(long chatId, String name) /* throws TelegramApiException */ {
		// String answer = "Hi, " + name + ", nice to meet you!";
		String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
		sendMessage(chatId, answer);
		log.info("Replied to user " + name);
	}

	private void sendMessage(long chatId, String textToSend) /* throws TelegramApiException */ {
		SendMessage messageToSend = new SendMessage();
		messageToSend.setChatId(String.valueOf(chatId));
		messageToSend.setText(textToSend);
		if (textToSend.equals(SHOW_KEYBOARD)) {
			ReplyKeyboardMarkup keyboardMarkup = showKeyboard();
			messageToSend.setReplyMarkup(keyboardMarkup);
		}
		if (textToSend.equals(REMOVE_KEYBOARD)) {
			//этот метод не работает, отложим до лучших времен
			ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
			messageToSend.setReplyMarkup(remove);
		}
		if (textToSend.equals(DELETE_DATA)) {
			InlineKeyboardMarkup markupInLine = showYesNoChice();
			messageToSend.setReplyMarkup(markupInLine);
		}
		try {
			execute(messageToSend);
		} catch (TelegramApiException ex) {
			log.error("Error occurred: " + ex.getMessage());
		}
	}

	private InlineKeyboardMarkup showYesNoChice() {
		InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
		List<InlineKeyboardButton> rowInLine = new ArrayList<>();
		var yesButton = new InlineKeyboardButton();
		yesButton.setText("Yes");
		yesButton.setCallbackData(YES_BUTTON);
		var noButton = new InlineKeyboardButton();
		noButton.setText("No");
		noButton.setCallbackData(NO_BUTTON);
		rowInLine.add(yesButton);
		rowInLine.add(noButton);
		rowsInLine.add(rowInLine);
		markupInLine.setKeyboard(rowsInLine);
		return markupInLine;
	}

	public ReplyKeyboardMarkup showKeyboard() {
		ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
		List<KeyboardRow> keyboardRows = new ArrayList<>();
		KeyboardRow row = new KeyboardRow();
		row.add("/start");
		row.add("/my_data");
		row.add("/delete_data");
		keyboardRows.add(row);
		row = new KeyboardRow();
		row.add("/help");
		row.add("/hide_keyboard_menu");
		keyboardRows.add(row);
		keyboardMarkup.setKeyboard(keyboardRows);
		return keyboardMarkup;
	}

	@Override
	public String getBotUsername() {
		return config.getBotName();
	}

	@Override
	public String getBotToken() {
		return config.getToken();
	}

}
