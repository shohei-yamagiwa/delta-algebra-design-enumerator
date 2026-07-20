package dev.shoheiyamagiwa.enumerator.gpu;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;

public final class VulkanContext implements AutoCloseable {
    private final VkInstance instance;
    private final VkPhysicalDevice physicalDevice;
    private final String physicalDeviceName;
    private final VkDevice device;
    private final VkQueue computeQueue;
    private final int computeQueueFamilyIndex;
    private final long commandPoolHandle;

    public VulkanContext() {
        this.instance = createInstance();

        try {
            this.physicalDevice = selectPhysicalDeviceWithCompute(this.instance);
            this.physicalDeviceName = readDeviceName(this.physicalDevice);
            this.computeQueueFamilyIndex = findComputeQueueFamilyIndex(this.physicalDevice);
            this.device = createLogicalDevice(this.physicalDevice, this.computeQueueFamilyIndex);
            this.computeQueue = retrieveComputeQueue(this.device, this.computeQueueFamilyIndex);
            this.commandPoolHandle = createCommandPool(this.device, this.computeQueueFamilyIndex);
        } catch (RuntimeException initializationFailure) {
            vkDestroyInstance(this.instance, null);
            throw initializationFailure;
        }
    }

    private static VkInstance createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .apiVersion(VK_API_VERSION_1_1);

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(applicationInfo);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            int result = vkCreateInstance(instanceCreateInfo, null, instancePointer);
            requireSuccess(result, "vkCreateInstance");

            return new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }
    }

    private static VkPhysicalDevice selectPhysicalDeviceWithCompute(VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);
            requireSuccess(vkEnumeratePhysicalDevices(instance, deviceCount, null), "vkEnumeratePhysicalDevices");

            int numberOfDevices = deviceCount.get(0);
            if (numberOfDevices == 0) {
                throw new IllegalStateException("No Vulkan physical devices were found.");
            }

            PointerBuffer devicePointers = stack.mallocPointer(numberOfDevices);
            requireSuccess(vkEnumeratePhysicalDevices(instance, deviceCount, devicePointers), "vkEnumeratePhysicalDevices");

            VkPhysicalDevice firstComputeCapableDevice = null;

            for (int deviceIndex = 0; deviceIndex < numberOfDevices; deviceIndex++) {
                VkPhysicalDevice candidate = new VkPhysicalDevice(devicePointers.get(deviceIndex), instance);

                if (!hasComputeQueueFamily(candidate)) {
                    continue;
                }

                int deviceType = readDeviceType(candidate);
                if (deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU || deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                    return candidate;
                }

                if (firstComputeCapableDevice == null) {
                    firstComputeCapableDevice = candidate;
                }
            }

            if (firstComputeCapableDevice == null) {
                throw new IllegalStateException("No compute queue family capable physical device was found.");
            }

            return firstComputeCapableDevice;
        }
    }

    private static boolean hasComputeQueueFamily(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);

            for (int familyIndex = 0; familyIndex < queueFamilies.capacity(); familyIndex++) {
                if ((queueFamilies.get(familyIndex).queueFlags() & VK_QUEUE_COMPUTE_BIT) != 0) {
                    return true;
                }
            }

            return false;
        }
    }

    private static int findComputeQueueFamilyIndex(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);

            for (int familyIndex = 0; familyIndex < queueFamilies.capacity(); familyIndex++) {
                if ((queueFamilies.get(familyIndex).queueFlags() & VK_QUEUE_COMPUTE_BIT) != 0) {
                    return familyIndex;
                }
            }

            throw new IllegalStateException("Couldn't find the compute queue family.");
        }
    }

    private static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, int computeQueueFamilyIndex) {
        try (MemoryStack stack = stackPush()) {
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(computeQueueFamilyIndex)
                    .pQueuePriorities(stack.floats(1.0f));

            VkPhysicalDeviceFeatures enabledFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            enabledFeatures.shaderInt64(true);

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .pEnabledFeatures(enabledFeatures);

            PointerBuffer devicePointer = stack.mallocPointer(1);
            requireSuccess(vkCreateDevice(physicalDevice, deviceCreateInfo, null, devicePointer), "vkCreateDevice");

            return new VkDevice(devicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    private static VkQueue retrieveComputeQueue(VkDevice device, int computeQueueFamilyIndex) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer queuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(device, computeQueueFamilyIndex, 0, queuePointer);

            return new VkQueue(queuePointer.get(0), device);
        }
    }

    private static long createCommandPool(VkDevice device, int computeQueueFamilyIndex) {
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(computeQueueFamilyIndex);

            LongBuffer commandPoolPointer = stack.mallocLong(1);
            requireSuccess(vkCreateCommandPool(device, commandPoolCreateInfo, null, commandPoolPointer), "vkCreateCommandPool");

            return commandPoolPointer.get(0);
        }
    }

    private static String readDeviceName(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            return deviceProperties.deviceNameString();
        }
    }

    private static int readDeviceType(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            return deviceProperties.deviceType();
        }
    }

    private static void requireSuccess(int resultCode, String operationName) {
        if (resultCode != VK_SUCCESS) {
            throw new IllegalStateException("Failed to " + operationName + " (VkResult=" + resultCode + ")。");
        }
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public String getPhysicalDeviceName() {
        return physicalDeviceName;
    }

    public VkQueue getComputeQueue() {
        return computeQueue;
    }

    public long getCommandPoolHandle() {
        return commandPoolHandle;
    }

    @Override
    public void close() {
        vkDestroyCommandPool(device, commandPoolHandle, null);
        vkDestroyDevice(device, null);
        vkDestroyInstance(instance, null);
    }
}
