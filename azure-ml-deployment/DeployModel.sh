#!/bin/bash

# Load config

. ./config.txt

# Azure Login 

az login

# Create ML workspace 

az ml workspace create -w $workspace_name -g $resource_group_name -l $location  -y --exist-ok

# Register the Model 

az ml model register -w $workspace_name --resource-group $resource_group_name -n $model_name --model-path $model_path -d "$description"


# Deploy Model using Inference and Deployment defined in json file

if [ $deployment_type == "ACI" ]; then
az ml model deploy -w $workspace_name --resource-group $resource_group_name --name $service_name -m $model_name:1  --ic inferenceconfig.json --dc deploymentconfigACI.json
elif [ $deployment_type == "AKS" ]; then
az ml computetarget create aks -n $aks_cluster -w $workspace_name --resource-group $resource_group_name
az ml model deploy -ct $aks_cluster -w $workspace_name --resource-group $resource_group_name -m $model_name:1 -n $service_name -ic inferenceconfig.json -dc deploymentconfigAKS.json
else
az ml model deploy -w $workspace_name --resource-group $resource_group_name -n $service_name -m $model_name:1 --ic inferenceconfig.json --dc deploymentconfigLocal.json
fi


#az ml service get-keys -n $service_name -w "$workspace_name" --resource-group $resource_group_name