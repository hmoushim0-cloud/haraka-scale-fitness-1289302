package sa.factory.harakascalefitness.di

import sa.factory.harakascalefitness.data.repository.HarakaScaleRepositoryImpl
import sa.factory.harakascalefitness.domain.repository.HarakaScaleRepository
import sa.factory.harakascalefitness.domain.usecase.*
import sa.factory.harakascalefitness.hardware.HardwareSensorDriver
import sa.factory.harakascalefitness.ui.viewmodel.HarakaScaleViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: HarakaScaleRepository = HarakaScaleRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): HarakaScaleViewModel = HarakaScaleViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
