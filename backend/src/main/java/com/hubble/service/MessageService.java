package com.hubble.service;

import com.hubble.dto.request.MessageRequest;
import com.hubble.entity.Message;
import com.hubble.mapper.MessageMapper;
import com.hubble.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

    MessageRepository messageRepository;
    MessageMapper messageMapper;
    SimpMessagingTemplate messagingTemplate;
    public Message sendMessage(MessageRequest request){
        Message newMessage = messageMapper.toMessage(request);
        Message savedMessage = messageRepository.save(newMessage);
        String destination = "/topic/channel/" + savedMessage.getChannelId();
        messagingTemplate.convertAndSend(destination, savedMessage);
        return savedMessage;
    }
    public List<Message> getMessagesByChannel(UUID channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable);
    }
}