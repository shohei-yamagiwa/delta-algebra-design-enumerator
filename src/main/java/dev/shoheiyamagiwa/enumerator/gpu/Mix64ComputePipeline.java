package dev.shoheiyamagiwa.enumerator.gpu;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class Mix64ComputePipeline implements AutoCloseable {
    public static final int PUSH_CONSTANT_SIZE_BYTES = 16;
    public static final int PUSH_CONSTANT_SEED_OFFSET = 8;

    private final VkDevice device;
    private final long shaderModuleHandle;
    private final long descriptorSetLayoutHandle;
    private final long pipelineLayoutHandle;
    private final long pipelineHandle;
    private final long descriptorPoolHandle;
    private final long descriptorSetHandle;

    public Mix64ComputePipeline(VkDevice device, byte[] spirvBytes, long outputBufferHandle) {
        this.device = device;
        this.shaderModuleHandle = createShaderModule(device, spirvBytes);

        try {
            this.descriptorSetLayoutHandle = createDescriptorSetLayout(device);
            this.pipelineLayoutHandle = createPipelineLayout(device, this.descriptorSetLayoutHandle);
            this.pipelineHandle = createComputePipeline(device, this.shaderModuleHandle, this.pipelineLayoutHandle);
            this.descriptorPoolHandle = createDescriptorPool(device);
            this.descriptorSetHandle = allocateDescriptorSet(device, this.descriptorPoolHandle, this.descriptorSetLayoutHandle);
            bindOutputBufferToDescriptorSet(device, this.descriptorSetHandle, outputBufferHandle);
        } catch (RuntimeException creationFailure) {
            closeQuietly();
            throw creationFailure;
        }
    }

    private static long createShaderModule(VkDevice device, byte[] spirvBytes) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer spirvBuffer = stack.malloc(spirvBytes.length);
            spirvBuffer.put(spirvBytes);
            spirvBuffer.flip();

            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(spirvBuffer);

            LongBuffer shaderModulePointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreateShaderModule(device, shaderModuleCreateInfo, null, shaderModulePointer), "vkCreateShaderModule");

            return shaderModulePointer.get(0);
        }
    }

    private static long createDescriptorSetLayout(VkDevice device) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            layoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);

            VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(layoutBindings);

            LongBuffer layoutPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreateDescriptorSetLayout(device, layoutCreateInfo, null, layoutPointer), "vkCreateDescriptorSetLayout");

            return layoutPointer.get(0);
        }
    }

    private static long createPipelineLayout(VkDevice device, long descriptorSetLayoutHandle) {
        try (MemoryStack stack = stackPush()) {
            VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
            pushConstantRanges.get(0)
                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                    .offset(0)
                    .size(PUSH_CONSTANT_SIZE_BYTES);

            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descriptorSetLayoutHandle))
                    .pPushConstantRanges(pushConstantRanges);

            LongBuffer pipelineLayoutPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreatePipelineLayout(device, pipelineLayoutCreateInfo, null, pipelineLayoutPointer), "vkCreatePipelineLayout");

            return pipelineLayoutPointer.get(0);
        }
    }

    private static long createComputePipeline(VkDevice device, long shaderModuleHandle, long pipelineLayoutHandle) {
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo shaderStageCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(shaderModuleHandle)
                    .pName(stack.UTF8("main"));

            VkComputePipelineCreateInfo.Buffer computePipelineCreateInfos = VkComputePipelineCreateInfo.calloc(1, stack);
            computePipelineCreateInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(shaderStageCreateInfo)
                    .layout(pipelineLayoutHandle);

            LongBuffer pipelinePointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(
                    vkCreateComputePipelines(device, VK_NULL_HANDLE, computePipelineCreateInfos, null, pipelinePointer),
                    "vkCreateComputePipelines");

            return pipelinePointer.get(0);
        }
    }

    private static long createDescriptorPool(VkDevice device) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1);

            VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(1);

            LongBuffer poolPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreateDescriptorPool(device, poolCreateInfo, null, poolPointer), "vkCreateDescriptorPool");

            return poolPointer.get(0);
        }
    }

    private static long allocateDescriptorSet(VkDevice device, long descriptorPoolHandle, long descriptorSetLayoutHandle) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPoolHandle)
                    .pSetLayouts(stack.longs(descriptorSetLayoutHandle));

            LongBuffer descriptorSetPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkAllocateDescriptorSets(device, allocateInfo, descriptorSetPointer), "vkAllocateDescriptorSets");

            return descriptorSetPointer.get(0);
        }
    }

    private static void bindOutputBufferToDescriptorSet(VkDevice device, long descriptorSetHandle, long outputBufferHandle) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1, stack);
            bufferInfos.get(0)
                    .buffer(outputBufferHandle)
                    .offset(0)
                    .range(VK_WHOLE_SIZE);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrites.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSetHandle)
                    .dstBinding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfos);

            vkUpdateDescriptorSets(device, descriptorWrites, null);
        }
    }

    public long getPipelineHandle() {
        return pipelineHandle;
    }

    public long getPipelineLayoutHandle() {
        return pipelineLayoutHandle;
    }

    public long getDescriptorSetHandle() {
        return descriptorSetHandle;
    }

    private void closeQuietly() {
        if (descriptorPoolHandle != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
        }
        if (pipelineHandle != VK_NULL_HANDLE) {
            vkDestroyPipeline(device, pipelineHandle, null);
        }
        if (pipelineLayoutHandle != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(device, pipelineLayoutHandle, null);
        }
        if (descriptorSetLayoutHandle != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(device, descriptorSetLayoutHandle, null);
        }
        if (shaderModuleHandle != VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, shaderModuleHandle, null);
        }
    }

    @Override
    public void close() {
        closeQuietly();
    }
}
