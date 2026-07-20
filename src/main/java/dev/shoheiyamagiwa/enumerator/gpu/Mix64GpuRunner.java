package dev.shoheiyamagiwa.enumerator.gpu;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class Mix64GpuRunner {
    private static final int WORKGROUP_SIZE = 256;

    private Mix64GpuRunner() {
        // No instantiation。
    }

    public static long[] run(VulkanContext context, Mix64ComputePipeline pipeline, StorageBuffer outputBuffer, int elementCount, long seed) {
        VkDevice device = context.getDevice();
        VkCommandBuffer commandBuffer = allocateCommandBuffer(device, context.getCommandPoolHandle());

        try {
            recordDispatch(commandBuffer, pipeline, elementCount, seed);
            submitAndWait(context, commandBuffer);

            return outputBuffer.readLongs(elementCount);
        } finally {
            freeCommandBuffer(device, context.getCommandPoolHandle(), commandBuffer);
        }
    }

    private static VkCommandBuffer allocateCommandBuffer(VkDevice device, long commandPoolHandle) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPoolHandle)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer commandBufferPointer = stack.mallocPointer(1);
            VulkanResults.requireSuccess(vkAllocateCommandBuffers(device, allocateInfo, commandBufferPointer), "vkAllocateCommandBuffers");

            return new VkCommandBuffer(commandBufferPointer.get(0), device);
        }
    }

    private static void recordDispatch(VkCommandBuffer commandBuffer, Mix64ComputePipeline pipeline, int elementCount, long seed) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VulkanResults.requireSuccess(vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getPipelineHandle());

            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.getPipelineLayoutHandle(),
                    0,
                    stack.longs(pipeline.getDescriptorSetHandle()),
                    null);

            ByteBuffer pushConstantData = buildPushConstantData(stack, elementCount, seed);
            vkCmdPushConstants(commandBuffer, pipeline.getPipelineLayoutHandle(), VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstantData);

            int workgroupCount = ceilDivide(elementCount, WORKGROUP_SIZE);
            vkCmdDispatch(commandBuffer, workgroupCount, 1, 1);

            VulkanResults.requireSuccess(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer");
        }
    }

    private static ByteBuffer buildPushConstantData(MemoryStack stack, int elementCount, long seed) {
        ByteBuffer pushConstantData = stack.malloc(Mix64ComputePipeline.PUSH_CONSTANT_SIZE_BYTES);

        pushConstantData.putInt(0, elementCount);
        pushConstantData.putLong(Mix64ComputePipeline.PUSH_CONSTANT_SEED_OFFSET, seed);

        return pushConstantData;
    }

    private static void submitAndWait(VulkanContext context, VkCommandBuffer commandBuffer) {
        VkDevice device = context.getDevice();

        try (MemoryStack stack = stackPush()) {
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);

            LongBuffer fencePointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreateFence(device, fenceCreateInfo, null, fencePointer), "vkCreateFence");

            long fenceHandle = fencePointer.get(0);

            try {
                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).pCommandBuffers(stack.pointers(commandBuffer));

                VulkanResults.requireSuccess(vkQueueSubmit(context.getComputeQueue(), submitInfo, fenceHandle), "vkQueueSubmit");

                long waitTimeoutNanoseconds = Long.MAX_VALUE;
                boolean waitForAllFences = true;
                VulkanResults.requireSuccess(vkWaitForFences(device, stack.longs(fenceHandle), waitForAllFences, waitTimeoutNanoseconds), "vkWaitForFences");
            } finally {
                vkDestroyFence(device, fenceHandle, null);
            }
        }
    }

    private static void freeCommandBuffer(VkDevice device, long commandPoolHandle, VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(device, commandPoolHandle, stack.pointers(commandBuffer));
        }
    }

    private static int ceilDivide(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
