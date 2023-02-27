// DataFeeder.cpp : Defines the entry point for the console application.
// Partially added kafka producer code from https://developer.confluent.io/get-started/c/#build-producer

#include <stdlib.h>

#include <iostream>
#include <string>

// #include <time.h>
#include <glib.h>
#include <librdkafka/rdkafka.h>
#include <unistd.h>

#include "LRDataProvider.h"
#include "common.c"

using namespace std;

void ErrorHandler(int nErrorCode) {
    switch (nErrorCode) {
        case END_OF_FILE: {
            cout << "End of data file" << endl;
        } break;
        case ERROR_FILE_NOT_FOUND: {
            cout << "Data file not found. Check data file path name." << endl;
        } break;
        case ERROR_INVALID_FILE: {
            cout << "Invalid file handler. Restart the system." << endl;
        } break;
        case ERROR_BUFFER_OVERFLOW: {
            cout << "Buffer over flow. Increase the buffer size." << endl;
        } break;
        default: {
            cout << "Programming error." << endl;
        } break;
    }
}

/* Optional per-message delivery callback (triggered by poll() or flush())
 * when a message has been successfully delivered or permanently
 * failed delivery (after retries).
 */
static void dr_msg_cb(rd_kafka_t* kafka_handle,
                      const rd_kafka_message_t* rkmessage,
                      void* opaque) {
    if (rkmessage->err) {
        g_error("Message delivery failed: %s", rd_kafka_err2str(rkmessage->err));
    }
}

int main(int argc, char* argv[]) {
    pthread_mutex_t mutex_lock = PTHREAD_MUTEX_INITIALIZER;

    // Check parameter
    if (argc < 2) {
        cout << "You have to provider input data file name as a parameter." << endl;
        return 0;
    }

    char* dataFile = argv[1];

    // CLRDataProvider
    CLRDataProvider* provider = new CLRDataProvider();

    // Initialize the provider
    cout << "Initializing..." << endl;
    int ret = provider->Initialize(dataFile, 10000, &mutex_lock);

    rd_kafka_t* producer;
    rd_kafka_conf_t* conf;
    char errstr[512];

    conf = rd_kafka_conf_new();

    // Install a delivery-error callback.
    rd_kafka_conf_set_dr_msg_cb(conf, dr_msg_cb);

    // Create the Producer instance.
    producer = rd_kafka_new(RD_KAFKA_PRODUCER, conf, errstr, sizeof(errstr));
    if (!producer) {
        g_error("Failed to create new producer: %s", errstr);
        return 1;
    }

    // Configuration object is now owned, and freed, by the rd_kafka_t instance.
    conf = NULL;

    // kafka topic to produce data to
    const char* topic = "lrb";
    int tuple_id = 0;

    // Allocate caller's buffer
    if (ret != SUCCESS) {
        ErrorHandler(ret);
        return 0;

    } else {
        // Using the provider
        if (provider->PrepareData(provider) == SUCCESS) {
            int nTuplesRead = 0;
            int nMaxTuples = 100;
            LPTuple lpTuples = new Tuple[nMaxTuples];

            // int seconds = 0;

            for (;;) {
                // Get a random number between 5 and 15
                srand(time(NULL));
                int s = (int)((((double)rand()) / RAND_MAX) * 10) + 5;

                // Sleep s seconds
                usleep(s);

                int ret;

                for (;;) {
                    // Gets available data
                    ret = provider->GetData(lpTuples, nMaxTuples, nTuplesRead);

                    if (ret < 0) {
                        // Handle erros including eof
                        ErrorHandler(ret);
                        break;
                    }

                    if (nTuplesRead == 0) {
                        // No tuple available
                        break;
                    }

                    // Using the return data
                    for (int i = 0; i < nTuplesRead; i++) {
                        ++tuple_id;
                        const int* key = &tuple_id;
                        const char* value = lpTuples[i].ToString();
                        size_t key_len = to_string(*key).length();
                        size_t value_len = strlen(value);

                        rd_kafka_resp_err_t err;

                        err = rd_kafka_producev(producer,
                                                RD_KAFKA_V_TOPIC(topic),
                                                RD_KAFKA_V_MSGFLAGS(RD_KAFKA_MSG_F_COPY),
                                                RD_KAFKA_V_KEY((void*)key, key_len),
                                                RD_KAFKA_V_VALUE((void*)value, value_len),
                                                RD_KAFKA_V_OPAQUE(NULL),
                                                RD_KAFKA_V_END);

                        if (err) {
                            g_error("Failed to produce to topic %s: %s", topic, rd_kafka_err2str(err));
                            return 1;
                        } else {
                            g_message("Produced event to topic %s: key = %d value = %12s", topic, *key, value);
                        }

                        rd_kafka_poll(producer, 0);

                        delete value;
                    }

                    if (nTuplesRead < nMaxTuples) {
                        // Last tuple has been read
                        break;
                    }
                }

                if (ret < SUCCESS) {
                    break;
                }
            }
        }

        // Block until the messages are all sent.
        g_message("Flushing final messages..");
        rd_kafka_flush(producer, 10 * 1000);

        if (rd_kafka_outq_len(producer) > 0) {
            g_error("%d message(s) were not delivered", rd_kafka_outq_len(producer));
        }

        g_message("%d events were produced to topic %s.", tuple_id, topic);

        rd_kafka_destroy(producer);

        // Uninitialize the provider
        cout << "Uninitialize..." << endl;
        provider->Uninitialize();
    }

    delete provider;

    return 0;
}