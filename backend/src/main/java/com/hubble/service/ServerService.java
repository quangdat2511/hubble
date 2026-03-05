package com.hubble.service;

import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerService {

    ServerRepository serverRepository;
    ServerMemberRepository serverMemberRepository;
}
