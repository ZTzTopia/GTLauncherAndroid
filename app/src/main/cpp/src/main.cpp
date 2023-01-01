#include <chrono>
#include <thread>
#include <dlfcn.h>
#include <dobby.h>

#include "game/hook.h"

__unused __attribute__((constructor))
void constructor_main()
{
    // Create a new thread because we don't want do while loop make main thread
    // stuck.
    auto thread = std::thread([]() {
        // Wait until Growtopia native library loaded.
        while (dlopen("libgrowtopia.so", RTLD_NOLOAD) == nullptr) {
            std::this_thread::sleep_for(std::chrono::milliseconds{ 32 });
        }

        // Starting to hook Growtopia function.
        game::hook::init();

        {
            struct BoostSignal {
                void* pad; // 0
                void* pad2; // 8
                void* pad3; // 16
                // ARM64 size!
            };

            struct BaseApp {
                BoostSignal pad[18]; // 0
                void* pad2; // 432
                bool consoleVisible; // 440
                bool fpsVisible; // 441
                // ARM64 size!
            };

            auto base_app{
                reinterpret_cast<BaseApp* (*)()>(
                    DobbySymbolResolver(nullptr, "_Z10GetBaseAppv")
                )()
            };
            base_app->fpsVisible = true;
        }
    });

    // Don't forget to detach the thread from the main thread.
    thread.detach();
}
