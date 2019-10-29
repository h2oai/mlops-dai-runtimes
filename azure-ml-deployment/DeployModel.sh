#!/bin/bash

# Load config

. ./config.txt

# Azure Login 

az login

# Create Resource Group and Storage Account

az group create --name $resource_group_name --location $location

storage_name=h2ostore${RANDOM}

az storage account create --name $storage_name --resource-group $resource_group_name --location $location --sku Standard_LRS --encryption blob

# Upload packages and modify dai_deploy.yml

storage_key=$(az storage account keys list --account-name $storage_name --resource-group $resource_group_name | python3 -c "import sys, json; print(json.load(sys.stdin)[0]['value'])")

export AZURE_STORAGE_ACCOUNT="${storage_name}"
export AZURE_STORAGE_KEY="${storage_key}"

az storage container create --name "mojodependencies" --public-access blob

dai_mojo_blob=$(basename -- "$daimojo_path")
datatable_blob=$(basename -- "$datatable_path")
az storage blob upload --container-name "mojodependencies" --name $dai_mojo_blob --file $daimojo_path
dai_mojo_url=$(az storage blob url --container-name "mojodependencies" --name $dai_mojo_blob | tr -d '"')
az storage blob upload --container-name "mojodependencies" --name $datatable_blob --file $datatable_path
datatable_url=$(az storage blob url --container-name "mojodependencies" --name $datatable_blob | tr -d '"')

cp dai_deploy_template.yml dai_deploy.yml
sed -i -E "s,@@DAI MOJO URL@@,${dai_mojo_url}," dai_deploy.yml
sed -i -E "s,@@DATA TABLE URLL@@,${datatable_url}," dai_deploy.yml

# Create ML workspace 

az ml workspace create -w $workspace_name -g $resource_group_name -l $location  -y --exist-ok

# Register the Model 

az ml model register -w $workspace_name --resource-group $resource_group_name -n $model_name --model-path $model_path -d "$description"


# Deploy Model using Inference and Deployment defined in json file

if [ $deployment_type == "ACI" ]; then
az ml model deploy -w $workspace_name --resource-group $resource_group_name --name $service_name -m $model_name:1  --ic inferenceconfig.json --dc deploymentconfigACI.json
elif [ $deployment_type == "AKS" ]; then
az ml computetarget create aks -n $aks_cluster -w $workspace_name --resource-group $resource_group_name
az ml model deploy --ct $aks_cluster -w $workspace_name --resource-group $resource_group_name -m $model_name:1 -n $service_name --ic inferenceconfig.json --dc deploymentconfigAKS.json
else
az ml model deploy -w $workspace_name --resource-group $resource_group_name -n $service_name -m $model_name:1 --ic inferenceconfig.json --dc deploymentconfigLocal.json
fi