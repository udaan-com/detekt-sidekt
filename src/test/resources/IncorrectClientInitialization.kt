import com.udaan.common.client.UdaanClientConfig
import com.udaan.common.client.UdaanServiceClient
import com.udaan.common.server.UdaanServerConfig
import javax.inject.Inject
import javax.inject.Named


class RandomClassWrongWay {
    companion object {
        private val someClientConfig: UdaanClientConfig = UdaanServerConfig["some-service"]!!
        val someServiceClient1 by lazy {  SomeServiceClient(someClientConfig) }
    }
}

class RandomClassRightWay {
    @Inject
    @Named("someServiceClient")
    private lateinit var someServiceClient2: SomeServiceClient
}

class SomeServiceClient(config: UdaanClientConfig) : UdaanServiceClient(config) {}