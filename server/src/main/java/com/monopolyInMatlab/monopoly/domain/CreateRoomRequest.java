package com.monopolyInMatlab.monopoly.domain;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateRoomRequest {
    private String roomName;
    private String hostUsername;
}
