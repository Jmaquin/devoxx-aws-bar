package com.bartender.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bartender.model.IotShadowState;
import com.bartender.model.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bartender.dao.GetFactureRepository;
import com.bartender.model.IotEventRequest;
import com.bartender.model.Command;

import static java.util.stream.Collectors.toList;

public class GetFactureService {
    private static final Logger LOG = LogManager.getLogger(GetFactureService.class);

    private GetFactureRepository getFactureRepository;

    public GetFactureService(GetFactureRepository getFactureRepository) {
        this.getFactureRepository = getFactureRepository;
    }

    public List<Command> handleInput(IotEventRequest iotEventRequest) {
        LOG.info("Got IOT event: {}", iotEventRequest.getCurrent());

        // TODO 05. get the reported and desired 'BarStatus'
        final String reportedBarStatus = getOrNull(() -> iotEventRequest.getCurrent().getState().getReported().getBarStatus());
        final String desiredBarStatus = getOrNull(() -> iotEventRequest.getCurrent().getState().getDesired().getBarStatus());

        if (reportedBarStatus != null
                && reportedBarStatus.equals(desiredBarStatus)
                && desiredBarStatus.equals(IotShadowState.CLOSED)) {
            // TODO 05. getCommands
            return getFactureRepository.getCommands(iotEventRequest.getDeviceId()).stream()
                    // TODO 05. update each command
                    .map(command -> getFactureRepository.saveCommand(updateCommand(command)))
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    private Command updateCommand(Command command) {
        // TODO 05. mark the beer and food as 'served'
        final Optional<Item> beer = Item
                .from(command.getBeer())
                .map(item -> item.setServed(true));

        final Optional<Item> food = Item
                .from(command.getFood())
                .map(item -> item.setServed(true));

        // TODO 05. build the command from the original command
        return Command.builder()
                .setClient(command.getClient())
                .setDateCommand(command.getDateCommand())
                .setBeer(beer.orElse(null))
                .setFood(food.orElse(null))
                .setId(command.getId())
                .build();
    }

    private String getOrNull(Supplier<String> f) {
        try {
            return f.get();
        } catch (Exception ex) {
            return null;
        }
    }
}
