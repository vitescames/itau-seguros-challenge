package br.com.itau.desafioseguros.application.command.handlers;

import br.com.itau.desafioseguros.application.command.AddInsuranceProductCommand;
import br.com.itau.desafioseguros.application.command.responses.AddInsuranceProductCommandResponse;
import br.com.itau.desafioseguros.application.command.validation.CommandValidator;
import br.com.itau.desafioseguros.application.event.EventBus;
import br.com.itau.desafioseguros.domain.entities.InsuranceProduct;
import br.com.itau.desafioseguros.domain.events.InsuranceProductCreated;
import br.com.itau.desafioseguros.domain.services.TariffedPriceCalculatorService;
import br.com.itau.desafioseguros.domain.valueobjects.InsuranceProductCategory;
import br.com.itau.desafioseguros.domain.valueobjects.InsuranceProductId;
import br.com.itau.desafioseguros.domain.repositories.AddInsuranceProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class AddInsuranceProductCommandHandler implements CommandHandler<AddInsuranceProductCommand, AddInsuranceProductCommandResponse> {

    private final TariffedPriceCalculatorService strategyFactory;
    private final CommandValidator validator;
    private final AddInsuranceProductRepository repository;
    private final EventBus eventBus;

    public AddInsuranceProductCommandHandler(TariffedPriceCalculatorService strategyFactory,
                                             CommandValidator validator,
                                             AddInsuranceProductRepository repository, EventBus eventBus) {
        this.strategyFactory = strategyFactory;
        this.validator = validator;
        this.repository = repository;
        this.eventBus = eventBus;
    }

    @Override
    public AddInsuranceProductCommandResponse handle(AddInsuranceProductCommand command) {
        validator.validate(command);

        InsuranceProductCategory categoryEnum = InsuranceProductCategory.valueOf(command.getCategory());

        BigDecimal tariffedPrice = strategyFactory.calculate(command.getBasePrice(), categoryEnum);

        InsuranceProduct insuranceProduct = InsuranceProduct.create(new InsuranceProductId(UUID.randomUUID()),
                command.getName(),
                categoryEnum,
                command.getBasePrice(),
                tariffedPrice);

        InsuranceProduct insuranceProductAdded = repository.add(insuranceProduct);

        insuranceProductAdded.registerEvent(new InsuranceProductCreated(insuranceProduct.getInsuranceProductId()));

        insuranceProductAdded.getDomainEvents().forEach(eventBus::publish);

        return new AddInsuranceProductCommandResponse(insuranceProductAdded.getInsuranceProductId().getId(),
                insuranceProductAdded.getName(),
                insuranceProductAdded.getCategory().toString(),
                insuranceProductAdded.getBasePrice(),
                insuranceProductAdded.getTariffedPrice().setScale(2, RoundingMode.CEILING));
    }
}
