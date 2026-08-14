param(
    [Parameter(Mandatory = $true)] [string] $CanonicalSourceRoot,
    [Parameter(Mandatory = $true)] [string] $TargetSourceRoot,
    [Parameter(Mandatory = $true)] [ValidateSet('mc-26.1.2')] [string] $Profile,
    [string] $CanonicalTestRoot,
    [string] $TargetTestRoot,
    [string] $TargetAuditSourceRoot,
    [switch] $AllowExistingTarget
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$canonical = (Resolve-Path -LiteralPath $CanonicalSourceRoot).Path
$target = [System.IO.Path]::GetFullPath($TargetSourceRoot)
$overrideRoot = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'neoforge/mc-26.1.2-nf-26.1.2.94/src/main/java'
if ([string]::IsNullOrWhiteSpace($target) -or $target -eq [System.IO.Path]::GetPathRoot($target)) {
    throw "Refusing to generate into an empty or filesystem-root target: $target"
}
if ((Test-Path -LiteralPath $target) -and -not $AllowExistingTarget) {
    throw "Target source directory already exists; use a fresh generated directory: $target"
}
New-Item -ItemType Directory -Path $target -Force | Out-Null

$compatBlockEntity = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity'
$compatItem = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatItem'
$compatBlockItem = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockItem'
$compatSwordItem = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatSwordItem'
$compatNbt = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt'
$compatAttributes = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatWeaponAttributes'
$legacySerializable = 'com.immortalstorage.immortalstorage.compat.mc2612.LegacyNbtSerializable'
$compatCodec = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec'
$compatValueIo = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo'
$compatPlayerInventory = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory'
$compatRecipeAccess = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess'
$compatPlayer = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer'
$compatCommands = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatCommands'
$compatTransfer = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer'
$compatMessages = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages'
$compatLevel = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel'
$compatTags = 'com.immortalstorage.immortalstorage.compat.mc2612.CompatTags'
$newline = [Environment]::NewLine

function Convert-2612RecipeNode([object] $node) {
    if ($null -eq $node) {
        return $null
    }
    if ($node -is [System.Array]) {
        $converted = New-Object System.Collections.Generic.List[object]
        foreach ($child in $node) {
            [void] $converted.Add((Convert-2612RecipeNode $child))
        }
        return ,$converted.ToArray()
    }
    if ($node -is [pscustomobject]) {
        $properties = @($node.PSObject.Properties)
        if ($properties.Count -eq 1 -and $properties[0].Name -eq 'item') {
            return [string] $properties[0].Value
        }
        if ($properties.Count -eq 1 -and $properties[0].Name -eq 'tag') {
            return '#' + [string] $properties[0].Value
        }
        $converted = [ordered]@{}
        foreach ($property in $properties) {
            $converted[$property.Name] = Convert-2612RecipeNode $property.Value
        }
        return $converted
    }
    return $node
}

function Convert-2612LootModifierNode([object] $node) {
    if ($null -eq $node) {
        return $null
    }
    if ($node -is [System.Array]) {
        $converted = New-Object System.Collections.Generic.List[object]
        foreach ($child in $node) {
            [void] $converted.Add((Convert-2612LootModifierNode $child))
        }
        return ,$converted.ToArray()
    }
    if ($node -is [pscustomobject]) {
        $converted = [ordered]@{}
        foreach ($property in @($node.PSObject.Properties)) {
            if ($property.Name -eq 'item' -and $property.Value -is [pscustomobject]) {
                $itemProperties = @($property.Value.PSObject.Properties)
                if ($itemProperties.Count -eq 1 -and $itemProperties[0].Name -eq 'item') {
                    $converted['item'] = [ordered]@{
                        id = [string] $itemProperties[0].Value
                        components = [ordered]@{}
                    }
                    continue
                }
            }
            $converted[$property.Name] = Convert-2612LootModifierNode $property.Value
        }
        return $converted
    }
    return $node
}

function Transform-2612([string] $text, [string] $relativePath) {
    # Minecraft 26.1 returns to Mojang's official names.  ResourceLocation is
    # now Identifier throughout the public API; keep the factory calls on the
    # official class as well instead of compiling against a guessed shim.
    $text = $text.Replace('net.minecraft.resources.ResourceLocation', 'net.minecraft.resources.Identifier')
    $text = $text.Replace('ResourceLocation.fromNamespaceAndPath', 'Identifier.fromNamespaceAndPath')
    $text = $text.Replace('ResourceLocation.withDefaultNamespace', 'Identifier.withDefaultNamespace')
    $text = $text.Replace('ResourceLocation.tryParse', 'Identifier.tryParse')
    $text = $text.Replace('ResourceLocation.parse', 'Identifier.parse')
    $text = $text.Replace('ResourceLocation.read', 'Identifier.read')
    $text = $text.Replace('ResourceLocation', 'Identifier')

    # 26.1 restores Mojang's official package spelling for advancement
    # predicates.  This is an official package move, not a local alias.
    $text = $text.Replace('net.minecraft.advancements.critereon', 'net.minecraft.advancements.criterion')

    # The 26.1 client renderer and GUI extractor packages were moved as part
    # of the official render-state migration.  Keep these imports aligned
    # with the target names before adapting the remaining call signatures.
    $text = $text.Replace('net.minecraft.client.renderer.RenderType',
        'net.minecraft.client.renderer.rendertype.RenderType')
    $text = $text.Replace('net.minecraft.client.gui.GuiGraphics',
        'net.minecraft.client.gui.GuiGraphicsExtractor')
    $text = [regex]::Replace($text, '\bGuiGraphics\b', 'GuiGraphicsExtractor')
    if ($relativePath -match '[\\/]client[\\/]screen[\\/].*Screen\.java$') {
        # Screen.extractRenderStateWithTooltipAndSubtitles already extracts
        # the background before invoking the container adapter. These legacy
        # calls otherwise produce the giant duplicated panel seen in 26.1.2.
        $text = [regex]::Replace($text,
            '(?m)^\s*renderBackground\(graphics, mouseX, mouseY, partialTick\);\r?\n', '')
        $text = $text.Replace(
            'renderBackground(graphics, mouseX, mouseY, partialTick); super.render(',
            'super.render(')
    }

    # The 1.21.1 source uses CustomData for arbitrary block/item payloads.
    # In 26.1.2 the old BLOCK_ENTITY_DATA component is typed for an actual
    # block-entity data wrapper, while arbitrary custom payloads belong to
    # CUSTOM_DATA.  This is an official component typing change, not a
    # compatibility probe.
    $text = $text.Replace('DataComponents.BLOCK_ENTITY_DATA', 'DataComponents.CUSTOM_DATA')

    $text = $text.Replace('net.minecraft.world.ItemInteractionResult', 'net.minecraft.world.InteractionResult')
    $text = $text.Replace('ItemInteractionResult', 'InteractionResult')
    # 26.1 requires this explicit result to continue into useWithoutItem.
    # Mapping it to plain PASS made machine menus fail whenever an item was held.
    $text = $text.Replace('PASS_TO_DEFAULT_BLOCK_INTERACTION', 'TRY_WITH_EMPTY_HAND')
    $text = $text.Replace('net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide)',
        '(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)')
    $text = $text.Replace('net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide)',
        '(player.level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)')
    $text = $text.Replace('InteractionResult.sidedSuccess(level.isClientSide)',
        '(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)')
    $text = $text.Replace('InteractionResult.sidedSuccess(player.level().isClientSide)',
        '(player.level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)')

    if ($relativePath -match '[\\/]block[\\/]ModBlocks\.java$') {
        # Item description ids are final in 26.1.2 and default to item.* even
        # for BlockItem. Select the official block prefix at construction so
        # the canonical block.* bilingual names remain authoritative.
        $text = $text.Replace('new Item.Properties()))',
            'new Item.Properties().useBlockDescriptionPrefix()))')
        $text = $text.Replace('new Item.Properties())));',
            'new Item.Properties().useBlockDescriptionPrefix())));')
    }

    # NeoForge's capability handles retain the same transfer interfaces in
    # 26.1, but their official nested capability names were shortened.
    $text = $text.Replace('Capabilities.ItemHandler', 'Capabilities.Item')
    $text = $text.Replace('Capabilities.FluidHandler', 'Capabilities.Fluid')
    $text = $text.Replace('Capabilities.EnergyStorage', 'Capabilities.Energy')

    # NeoForge 26.1 capability lookups return the official transfer APIs.
    # The shared implementation still uses the legacy handler contracts
    # internally, so adapt only at these explicit capability boundaries.
    $text = $text.Replace('level.getCapability(Capabilities.Item.BLOCK, pos, face)',
        "$script:compatTransfer.itemHandler(level.getCapability(Capabilities.Item.BLOCK, pos, face))")
    $text = $text.Replace('level.getCapability(Capabilities.Fluid.BLOCK, pos, face)',
        "$script:compatTransfer.fluidHandler(level.getCapability(Capabilities.Fluid.BLOCK, pos, face))")
    $text = $text.Replace('level.getCapability(Capabilities.Energy.BLOCK, pos, face)',
        "$script:compatTransfer.energyHandler(level.getCapability(Capabilities.Energy.BLOCK, pos, face))")
    $text = $text.Replace('level.getCapability(Capabilities.Item.BLOCK, target, face)',
        "$script:compatTransfer.itemHandler(level.getCapability(Capabilities.Item.BLOCK, target, face))")
    $text = $text.Replace('level.getCapability(Capabilities.Item.BLOCK, targetPos, side.getOpposite())',
        "$script:compatTransfer.itemHandler(level.getCapability(Capabilities.Item.BLOCK, targetPos, side.getOpposite()))")
    $text = $text.Replace('serverLevel.getCapability(Capabilities.Item.BLOCK, targetPos, side.getOpposite())',
        "$script:compatTransfer.itemHandler(serverLevel.getCapability(Capabilities.Item.BLOCK, targetPos, side.getOpposite()))")
    $text = $text.Replace('serverLevel.getCapability(Capabilities.Fluid.BLOCK, targetPos, side.getOpposite())',
        "$script:compatTransfer.fluidHandler(serverLevel.getCapability(Capabilities.Fluid.BLOCK, targetPos, side.getOpposite()))")
    $text = $text.Replace('serverLevel.getCapability(Capabilities.Energy.BLOCK, targetPos, side.getOpposite())',
        "$script:compatTransfer.energyHandler(serverLevel.getCapability(Capabilities.Energy.BLOCK, targetPos, side.getOpposite()))")
    $text = $text.Replace('level.getCapability(`r`n                Capabilities.Fluid.BLOCK, pos, face)',
        "$script:compatTransfer.fluidHandler(level.getCapability(`r`n                Capabilities.Fluid.BLOCK, pos, face))")
    $text = $text.Replace('level.getCapability(`r`n                Capabilities.Energy.BLOCK, pos, face)',
        "$script:compatTransfer.energyHandler(level.getCapability(`r`n                Capabilities.Energy.BLOCK, pos, face))")
    $text = $text.Replace('sl.getCapability(Capabilities.Item.BLOCK,`r`n                    worldPosition.relative(dir), dir.getOpposite())',
        "$script:compatTransfer.itemHandler(sl.getCapability(Capabilities.Item.BLOCK,`r`n                    worldPosition.relative(dir), dir.getOpposite()))")
    $text = $text.Replace('sl.getCapability(Capabilities.Fluid.BLOCK,`r`n                    worldPosition.relative(dir), dir.getOpposite())',
        "$script:compatTransfer.fluidHandler(sl.getCapability(Capabilities.Fluid.BLOCK,`r`n                    worldPosition.relative(dir), dir.getOpposite()))")

    $text = $text.Replace('sl.getCapability(Capabilities.Item.BLOCK,' + $newline +
            '                    worldPosition.relative(dir), dir.getOpposite())',
        "$script:compatTransfer.itemHandler(sl.getCapability(Capabilities.Item.BLOCK," + $newline +
            '                    worldPosition.relative(dir), dir.getOpposite()))')
    $text = $text.Replace('sl.getCapability(Capabilities.Fluid.BLOCK,' + $newline +
            '                    worldPosition.relative(dir), dir.getOpposite())',
        "$script:compatTransfer.fluidHandler(sl.getCapability(Capabilities.Fluid.BLOCK," + $newline +
            '                    worldPosition.relative(dir), dir.getOpposite()))')
    foreach ($capability in @(
        @{ Name = 'Item'; Adapter = 'itemHandler' },
        @{ Name = 'Fluid'; Adapter = 'fluidHandler' },
        @{ Name = 'Energy'; Adapter = 'energyHandler' })) {
        $old = 'level.getCapability(' + $newline +
            '                Capabilities.' + $capability.Name + '.BLOCK, pos, face)'
        $new = "$script:compatTransfer.$($capability.Adapter)(level.getCapability(" + $newline +
            '                Capabilities.' + $capability.Name + '.BLOCK, pos, face))'
        $text = $text.Replace($old, $new)
    }
    foreach ($capability in @(
        @{ Name = 'Item'; Adapter = 'itemHandler' },
        @{ Name = 'Fluid'; Adapter = 'fluidHandler' },
        @{ Name = 'Energy'; Adapter = 'energyHandler' })) {
        $receiver = if ($capability.Name -eq 'Item') { 'itemHandler' } elseif ($capability.Name -eq 'Fluid') { 'fluidHandler' } else { 'energyHandler' }
        foreach ($owner in @('serverLevel', 'level')) {
            $old = $owner + '.getCapability(' + $newline +
                '                Capabilities.' + $capability.Name + '.BLOCK, targetPos, side.getOpposite()' + $newline +
                '        )'
            $new = "$script:compatTransfer.$receiver(" + $owner + '.getCapability(' + $newline +
                '                Capabilities.' + $capability.Name + '.BLOCK, targetPos, side.getOpposite()' + $newline +
                '        ))'
            $text = $text.Replace($old, $new)
        }
    }

    # The canonical source also contains capability lookups whose arguments
    # are split across lines but whose closing parenthesis stays on the same
    # line as the final argument. Adapt those typed assignments as a whole;
    # the negative lookahead keeps the earlier single-line replacements from
    # being wrapped a second time.
    $text = [regex]::Replace($text,
        '(?s)(IItemHandler\s+\w+\s*=\s*)(?!' + [regex]::Escape($compatTransfer) + '\.itemHandler\()((?:level|serverLevel|sl)\.getCapability\(\s*Capabilities\.Item\.BLOCK,.*?\))(?=;)',
        '$1' + $compatTransfer + '.itemHandler($2)')
    $text = [regex]::Replace($text,
        '(?s)(IFluidHandler\s+\w+\s*=\s*)(?!' + [regex]::Escape($compatTransfer) + '\.fluidHandler\()((?:level|serverLevel|sl)\.getCapability\(\s*Capabilities\.Fluid\.BLOCK,.*?\))(?=;)',
        '$1' + $compatTransfer + '.fluidHandler($2)')
    $text = [regex]::Replace($text,
        '(?s)(IEnergyStorage\s+\w+\s*=\s*)(?!' + [regex]::Escape($compatTransfer) + '\.energyHandler\()((?:level|serverLevel|sl)\.getCapability\(\s*Capabilities\.Energy\.BLOCK,.*?\))(?=;)',
        '$1' + $compatTransfer + '.energyHandler($2)')

    # Client-originated payloads moved to the explicit client distributor.
    # Server-to-player broadcasts still use PacketDistributor.
    if ($text.Contains('PacketDistributor.sendToServer(')) {
        $text = $text.Replace('import net.neoforged.neoforge.network.PacketDistributor;',
            'import net.neoforged.neoforge.client.network.ClientPacketDistributor;')
        $text = $text.Replace('PacketDistributor.sendToServer(', 'ClientPacketDistributor.sendToServer(')
    }

    $text = $text.Replace('net.neoforged.neoforge.common.util.INBTSerializable', $script:legacySerializable)
    $text = $text.Replace('implements INBTSerializable<CompoundTag>', "implements $script:legacySerializable<CompoundTag>")

    if ($relativePath -match 'block[\\/]entity[\\/]') {
        $text = [regex]::Replace($text,
            '(?m)(\bclass\s+\w+(?:\s*<[^{}]*>)?\s+extends\s+)BlockEntity\b',
            "`$1$script:compatBlockEntity")
        $text = [regex]::Replace($text,
            '(?m)((?:protected|public)\s+(?:final\s+)?void\s+)saveAdditional\s*\(CompoundTag',
            '$1saveAdditionalLegacy(CompoundTag')
        $text = [regex]::Replace($text,
            '(?m)((?:protected|public)\s+(?:final\s+)?void\s+)loadAdditional\s*\(CompoundTag',
            '$1loadAdditionalLegacy(CompoundTag')
        $text = $text.Replace('.saveAdditional(', '.saveAdditionalLegacy(')
        $text = $text.Replace('.loadAdditional(', '.loadAdditionalLegacy(')
        $text = [regex]::Replace($text, '(?<![\w.])saveAdditional\(', 'saveAdditionalLegacy(')
        $text = [regex]::Replace($text, '(?<![\w.])loadAdditional\(', 'loadAdditionalLegacy(')
    }

    # CompoundTag/ListTag getters became Optional-returning in 26.1.2.
    $text = $text.Replace('CompoundTag.TAG_', 'Tag.TAG_')
    $text = [regex]::Replace($text, '\.contains\(([^,()\r\n]+),\s*(?:net\.minecraft\.nbt\.)?Tag\.TAG_[A-Z_]+\)', '.contains($1)')
    $text = [regex]::Replace($text, '\.getInt\(([^()\r\n]+)\)', '.getIntOr($1, 0)')
    $text = [regex]::Replace($text, '\.getLong\(([^()\r\n]+)\)', '.getLongOr($1, 0L)')
    $text = [regex]::Replace($text, '\.getBoolean\(([^()\r\n]+)\)', '.getBooleanOr($1, false)')
    $text = [regex]::Replace($text, '\.getDouble\(([^()\r\n]+)\)', '.getDoubleOr($1, 0.0D)')
    $text = $text.Replace('.getList(', '.getListOrEmpty(')
    $text = $text.Replace('.getCompound(', '.getCompoundOrEmpty(')
    $text = [regex]::Replace($text, '\.getListOrEmpty\(([^,()\r\n]+),\s*(?:net\.minecraft\.nbt\.)?Tag\.TAG_[A-Z_]+\)', '.getListOrEmpty($1)')
    $text = [regex]::Replace($text, '\.getIntArray\(([^()\r\n]*)\)', '.getIntArray($1).orElseGet(() -> new int[0])')
    $text = [regex]::Replace($text, '\.getLongArray\(([^()\r\n]*)\)', '.getLongArray($1).orElseGet(() -> new long[0])')
    # Avoid rewriting Component#getString() and preserve Brigadier's static
    # StringArgumentType#getString(...) while adapting CompoundTag#getString.
    $text = $text.Replace('StringArgumentType.getString(', '__IMMORTAL_STRING_ARGUMENT_GET__(')
    $text = [regex]::Replace($text, '\.getString\(([^()\r\n]+)\)', '.getStringOr($1, "")')
    $text = $text.Replace('__IMMORTAL_STRING_ARGUMENT_GET__(', 'StringArgumentType.getString(')

    # Mojang's 26.1 GUI extractor retained the rendering operations but gave
    # them explicit extractor names.  Keep the call-site conversion here so
    # all screens and painters share the same official mapping.
    $text = $text.Replace('.drawString(', '.text(')
    $text = $text.Replace('.drawCenteredString(', '.centeredText(')
    $text = $text.Replace('.drawWordWrap(', '.textWithWordWrap(')
    $text = $text.Replace('.hLine(', '.horizontalLine(')
    $text = $text.Replace('.vLine(', '.verticalLine(')
    $text = $text.Replace('.renderFakeItem(', '.fakeItem(')
    $text = $text.Replace('.renderItemDecorations(', '.itemDecorations(')
    $text = $text.Replace('.renderItem(', '.item(')
    $text = $text.Replace('.renderOutline(', '.outline(')

    # Recipe assembly no longer receives a registry-access argument in the
    # official 26.1 recipe contract.  Keep the input object and drop only the
    # documented second argument at target call sites.
    $text = [regex]::Replace($text,
        '(\.assemble\([^;\r\n]*?),\s*(?:net\.minecraft\.client\.)?Minecraft\.getInstance\(\)\.level\.registryAccess\(\)\)',
        '$1)')
    $text = $text.Replace('.assemble(input, serverPlayer.level().registryAccess())',
        '.assemble(input)')
    $text = $text.Replace('.assemble(input, level.registryAccess())',
        '.assemble(input)')

    # The 26.1 record-shaped ChunkPos uses the official containing/pack
    # factories rather than the removed BlockPos constructor and asLong name.
    $text = $text.Replace('new ChunkPos(pos)', 'ChunkPos.containing(pos)')
    $text = $text.Replace('new ChunkPos(spawnPos)', 'ChunkPos.containing(spawnPos)')
    $text = $text.Replace('ChunkPos.asLong(', 'ChunkPos.pack(')

    # Official 26.1 renamed the collision-property builder and SpawnEggItem's
    # registry-aware lookup was removed because the item already carries its
    # entity type.
    $text = $text.Replace('.noCollission()', '.noCollision()')
    $text = $text.Replace('.getType(level.registryAccess(), ', '.getType(')
    $text = $text.Replace('.getType(displayLevel.registryAccess(), ', '.getType(')
    $text = $text.Replace('.getType(registries, ', '.getType(')
    $text = $text.Replace('.getWindow().getWindow()', '.getWindow()')
    $text = $text.Replace('ClickType', 'ContainerInput')
    $text = $text.Replace('FMLEnvironment.dist', 'FMLEnvironment.getDist()')
    $text = $text.Replace('net.minecraft.world.entity.LivingEntity.getSlotForHand(', "$script:compatPlayer.slotForHand(")
    $text = $text.Replace('LivingEntity.getSlotForHand(', "$script:compatPlayer.slotForHand(")
    $text = $text.Replace('sp.hasPermissions(2)', "$script:compatPlayer.hasPermissions(sp, 2)")
    $text = $text.Replace('player.hasPermissions(2)', "$script:compatPlayer.hasPermissions(player, 2)")
    $text = $text.Replace('source.hasPermission(2)', "$script:compatCommands.hasPermission(source, 2)")
    $text = $text.Replace('.getDayTime()', '.getOverworldClockTime()')
    $text = $text.Replace('modifiedRealmChunks.add(pos.toLong())', 'modifiedRealmChunks.add(pos.pack())')

    # IntProvider's public accessors and codec moved to the plural official
    # helper in 26.1.2.
    $text = $text.Replace('.getMinValue()', '.minInclusive()')
    $text = $text.Replace('.getMaxValue()', '.maxInclusive()')
    $text = $text.Replace('IntProvider.CODEC', 'net.minecraft.util.valueproviders.IntProviders.CODEC')

    foreach ($receiver in @('tag', 'entry', 'persistentData', 'migrated', 'entityTag')) {
        $text = $text.Replace("$receiver.hasUUID(", "$script:compatNbt.hasUuid($receiver, ")
        $text = $text.Replace("$receiver.getUUID(", "$script:compatNbt.getUuid($receiver, ")
        $text = $text.Replace("$receiver.putUUID(", "$script:compatNbt.putUuid($receiver, ")
    }
    $text = [regex]::Replace($text,
        '(\w+\.getPersistentData\(\))\.hasUUID\(([^)]*)\)',
        { param($match) "$script:compatNbt.hasUuid($($match.Groups[1].Value), $($match.Groups[2].Value))" })

    # Registry#get now returns an Optional holder for item and fluid lookups.
    # Apply fully-qualified occurrences first, then only match unqualified
    # BuiltInRegistries tokens whose preceding character is not the package dot.
    foreach ($registry in @('ITEM', 'FLUID')) {
        $valueMapper = 'map(net.minecraft.core.Holder.Reference::value).orElse(null)'
        $qualifiedPrefix = 'net.minecraft.core.registries.BuiltInRegistries'
        foreach ($argument in @('id', 'definition.outputId()')) {
            $old = "$qualifiedPrefix.$registry.get($argument)"
            $text = $text.Replace($old, "$old.$valueMapper")
            $old = "BuiltInRegistries.$registry.get($argument)"
            $text = [regex]::Replace($text,
                "(?<![\w.])BuiltInRegistries\.$registry\.get\($([regex]::Escape($argument))\)",
                "$old.$valueMapper")
        }
    }

    # Item use/animation and material APIs were collapsed into component-based APIs.
    $text = $text.Replace('net.minecraft.world.InteractionResultHolder', 'net.minecraft.world.InteractionResult')
    $text = $text.Replace('InteractionResultHolder<ItemStack>', 'InteractionResult')
    $text = [regex]::Replace($text,
        'InteractionResultHolder\.sidedSuccess\((?:stack|bottle|held),\s*level\.isClientSide\)',
        '(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)')
    $text = [regex]::Replace($text, 'InteractionResultHolder\.(success|pass|fail|consume)\(\w+\)', {
        param($match)
        switch ($match.Groups[1].Value) {
            'success' { 'InteractionResult.SUCCESS' }
            'pass' { 'InteractionResult.PASS' }
            'fail' { 'InteractionResult.FAIL' }
            default { 'InteractionResult.CONSUME' }
        }
    })
    $text = $text.Replace('InteractionResult.consume(player.getItemInHand(hand))',
        'InteractionResult.CONSUME')
    # Do not replace the method name getUseAnimation; only the old enum type
    # and its constants changed in 26.1.2.
    $text = $text.Replace('net.minecraft.world.item.UseAnim', 'net.minecraft.world.item.ItemUseAnimation')
    $text = $text.Replace('UseAnim.BOW', 'ItemUseAnimation.BOW')
    $text = $text.Replace('net.minecraft.world.item.Tiers', 'net.minecraft.world.item.ToolMaterial')
    $text = $text.Replace('net.minecraft.world.item.Tier', 'net.minecraft.world.item.ToolMaterial')
    $text = $text.Replace('ToolMaterials', 'ToolMaterial')
    $text = $text.Replace('Tier ', 'ToolMaterial ')
    $text = $text.Replace('Tiers.NETHERITE', 'ToolMaterial.NETHERITE')
    $text = $text.Replace('DiggerItem.createAttributes(ToolMaterial.NETHERITE, 4.0F, -2.4F)',
        "$script:compatAttributes.toolAttributes(ToolMaterial.NETHERITE, 4.0F, -2.4F)")
    $text = $text.Replace('import net.minecraft.world.item.DiggerItem;', '')
    $text = $text.Replace('import net.minecraft.world.item.EnchantedBookItem;', '')
    $text = $text.Replace('EnchantedBookItem.createForEnchantment(',
        'net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(')
    $text = $text.Replace('net.minecraft.world.item.net.minecraft.world.item.enchantment.EnchantmentHelper',
        'net.minecraft.world.item.enchantment.EnchantmentHelper')
    $text = $text.Replace('import net.minecraft.world.item.crafting.SimpleCookingSerializer;', '')
    $text = $text.Replace('new SimpleCookingSerializer<>(', 'new AbstractCookingRecipe.Serializer<>(')

    # The new block-entity registration is a direct varargs constructor.
    $text = $text.Replace('BlockEntityType.Builder.of(', 'new BlockEntityType<>(')
    $text = $text.Replace(').build(null)', ')')

    # Server reload listeners now require stable keys. Keep every canonical
    # listener and give it a deterministic id so reload ordering remains
    # observable and duplicate-free.
    $text = $text.Replace('net.neoforged.neoforge.event.AddReloadListenerEvent',
        'net.neoforged.neoforge.event.AddServerReloadListenersEvent')
    $reloadKeys = @(
        @('event.addListener(new com.immortalstorage.immortalstorage.worldshard.WorldShardMinerReloadListener(',
            'event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, "world_shard_miner"), new com.immortalstorage.immortalstorage.worldshard.WorldShardMinerReloadListener('),
        @('event.addListener(new com.immortalstorage.immortalstorage.worldshard.WorldShardLootReloadListener())',
            'event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, "world_shard_loot"), new com.immortalstorage.immortalstorage.worldshard.WorldShardLootReloadListener())'),
        @('event.addListener(new com.immortalstorage.immortalstorage.worldshard.WorldShardLootReloadListener(',
            'event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, "world_shard_loot"), new com.immortalstorage.immortalstorage.worldshard.WorldShardLootReloadListener('),
        @('event.addListener(new com.immortalstorage.immortalstorage.source.definition.SourceDefinitionReloadListener())',
            'event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, "source_definitions"), new com.immortalstorage.immortalstorage.source.definition.SourceDefinitionReloadListener())'),
        @('event.addListener(new com.immortalstorage.immortalstorage.spiritfield.SimulatedSpiritFieldCropCatalog.ReloadListener())',
            'event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, "simulated_spirit_field_crops"), new com.immortalstorage.immortalstorage.spiritfield.SimulatedSpiritFieldCropCatalog.ReloadListener())')
    )
    foreach ($replacement in $reloadKeys) {
        $text = $text.Replace($replacement[0], $replacement[1])
    }

    # Level and recipe-manager access are exposed through the recipe-access
    # facade in this release. Collection queries are rebuilt by the adapter so
    # priority and fallback selection remain identical to 1.21.1.
    $text = $text.Replace('level.getRecipeManager()', 'level.recipeAccess()')
    $text = $text.Replace('player.level().getRecipeManager()', 'player.level().recipeAccess()')
    $text = $text.Replace('serverPlayer.level().getRecipeManager()', 'serverPlayer.level().recipeAccess()')
    $text = $text.Replace('manager.getRecipesFor(',
        "$script:compatRecipeAccess.getRecipesFor(manager, ")
    $text = $text.Replace('serverPlayer.server.recipeAccess().getRecipesFor(',
        "$script:compatRecipeAccess.getRecipesFor(serverPlayer.server.recipeAccess(), ")
    $text = $text.Replace('serverPlayer.server.getRecipeManager().getRecipesFor(',
        "$script:compatRecipeAccess.getRecipesFor(serverPlayer.server.getRecipeManager(), ")
    $text = [regex]::Replace($text,
        'serverPlayer\.server\.getRecipeManager\(\)\s*\.getRecipesFor\(',
        "$script:compatRecipeAccess.getRecipesFor(serverPlayer.server.getRecipeManager(), ")
    $text = $text.Replace('player.level().recipeAccess().getRecipesFor(',
        "$script:compatRecipeAccess.getRecipesFor(player.level().recipeAccess(), ")
    $text = $text.Replace('level.recipeAccess().getRecipesFor(',
        "$script:compatRecipeAccess.getRecipesFor(level.recipeAccess(), ")
    $text = $text.Replace('manager.getAllRecipesFor(',
        "$script:compatRecipeAccess.getAllRecipesFor(manager, ")
    $text = $text.Replace('level.recipeAccess().getAllRecipesFor(',
        "$script:compatRecipeAccess.getAllRecipesFor(level.recipeAccess(), ")
    $text = $text.Replace('player.level().recipeAccess().getAllRecipesFor(',
        "$script:compatRecipeAccess.getAllRecipesFor(player.level().recipeAccess(), ")
    $text = $text.Replace('registry.getRecipeManager().getAllRecipesFor(',
        "$script:compatRecipeAccess.getAllRecipesFor(registry.getRecipeManager(), ")
    $text = $text.Replace('manager.byKey(entry.getKey())',
        "$script:compatRecipeAccess.byKey(manager, entry.getKey())")
    $text = $text.Replace('level.recipeAccess().byKey(entry.getKey())',
        "$script:compatRecipeAccess.byKey(level.recipeAccess(), entry.getKey())")
    $text = $text.Replace('.getExperience()', '.experience()')

    # 26.1.2 exposes inclusive height and block-state light opacity directly.
    $text = $text.Replace('.getMaxBuildHeight()', '.getMaxY() + 1')
    $text = $text.Replace('.getMinBuildHeight()', '.getMinY()')
    $text = $text.Replace('.getLightBlock(level, cursor)', '.getLightBlock()')

    # Common NeoForge registries now return Optional holders. Mekanism's
    # chemical registry follows the same target-version contract.
    $text = $text.Replace('MekanismAPI.CHEMICAL_REGISTRY.get(id)',
        'MekanismAPI.CHEMICAL_REGISTRY.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null)')
    $text = $text.Replace('net.neoforged.neoforge.common.util.TriState', 'net.minecraft.util.TriState')
    $text = $text.Replace('net.minecraft.world.entity.MobSpawnType', 'net.minecraft.world.entity.EntitySpawnReason')
    $text = $text.Replace('MobSpawnType.', 'EntitySpawnReason.')

    # ItemCooldowns is stack-keyed in the target API. Preserve the exact held
    # stack so cooldown groups remain component-sensitive.
    $text = [regex]::Replace($text,
        '(?<![\w.])(\w+)\.getCooldowns\(\)\.addCooldown\((\w+)\.getItem\(\),',
        { param($match) "$($match.Groups[1].Value).getCooldowns().addCooldown($($match.Groups[2].Value)," })

    $text = $text.Replace('player.getInventory().selected',
        'player.getInventory().getSelectedSlot()')

    # Official 26.1 turns several formerly exposed fields into accessors. The
    # replacements below are limited to the documented Level/ResourceKey
    # contracts and are applied after all sided-success expressions have been
    # rewritten, so existing method calls remain untouched.
    $text = [regex]::Replace($text, '\.isClientSide\b(?!\s*\()', '.isClientSide()')
    $text = $text.Replace('.dimension().location()', '.dimension().identifier()')
    $text = $text.Replace('Level.OVERWORLD.location()', 'Level.OVERWORLD.identifier()')
    $text = $text.Replace('Level.NETHER.location()', 'Level.NETHER.identifier()')
    $text = $text.Replace('Level.END.location()', 'Level.END.identifier()')
    $text = $text.Replace('.id().location()', '.id().identifier()')
    $text = $text.Replace('key.location()', 'key.identifier()')
    $text = $text.Replace('dimension.location()', 'dimension.identifier()')
    $text = [regex]::Replace($text, '(?<![\w.])([A-Za-z_]\w*)\.random\b', '$1.getRandom()')
    $text = $text.Replace('.getGameProfile().getName()', '.getGameProfile().name()')
    foreach ($receiver in @('serverPlayer', 'player', 'owner', 'actor', 'sp', 'p')) {
        $escapedReceiver = [regex]::Escape($receiver)
        $text = [regex]::Replace($text, "(?<![A-Za-z0-9_])$escapedReceiver\.getServer\(\)",
            "${receiver}.level().getServer()")
        $text = [regex]::Replace($text, "(?<![A-Za-z0-9_])$escapedReceiver\.displayClientMessage\(",
            "$compatMessages.sendSystemMessage(${receiver}, ")
    }
    $text = [regex]::Replace($text, '(?<![A-Za-z0-9_])level\.getServer\(\)',
        "$compatLevel.server(level)")
    $text = $text.Replace('context.level().getServer()', "$compatLevel.server(context.level())")
    $text = $text.Replace('realm.getServer()', "$compatLevel.server(realm)")
    $text = $text.Replace('stack.getTags()', "$compatTags.getTags(stack)")
    $text = $text.Replace('net.minecraft.world.entity.LivingEntity.getSlotForHand(',
        "$compatPlayer.slotForHand(")
    $text = $text.Replace('.canInteractWithBlock(', ".canInteractWithBlock(")
    $text = [regex]::Replace($text,
        '(?<![\w.])([A-Za-z_]\w*)\.canInteractWithBlock\(',
        "$compatPlayer.canInteractWithBlock(`$1, ")

    if ($relativePath -match '[\\/]menu[\\/]custom[\\/]TerminalRecipeTransfer\.java$') {
        $text = $text.Replace('!recipe.value().canCraftInDimensions(3, 3)',
            'recipe.value().placementInfo().isImpossibleToPlace()')
        $text = $text.Replace('List<Ingredient> layout = recipe.value().getIngredients();',
            'List<Ingredient> layout = recipe.value().placementInfo().ingredients();')
        $text = $text.Replace('java.util.Arrays.stream(ingredient.getItems())',
            'ingredient.items()')
        $text = $text.Replace('.mapToInt(ItemStack::getMaxStackSize)',
            '.mapToInt(holder -> holder.value().getMaxStackSize(new ItemStack(holder.value())))')
    }
    if ($relativePath -match '[\\/]network[\\/]ModNetwork\.java$') {
        $text = $text.Replace('.byKey(payload.recipeId())',
            '.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, payload.recipeId()))')
        $text = $text.Replace('recipe.getIngredients()', 'recipe.placementInfo().ingredients()')
    }
    if ($relativePath -match '[\\/]ImmortalStorageMod\.java$') {
        # GatherDataEvent became abstract in 26.1.2. Register both concrete
        # lifecycle subclasses so the data callback remains available on
        # client and server without asking the event bus to bind an abstract
        # event type.
        $text = $text.Replace('modBus.addListener(ModDataGeneration::gatherData);',
            'modBus.addListener(net.neoforged.neoforge.data.event.GatherDataEvent.Client.class, ModDataGeneration::gatherData);' + $newline +
            '        modBus.addListener(net.neoforged.neoforge.data.event.GatherDataEvent.Server.class, ModDataGeneration::gatherData);')
        $text = $text.Replace('        NeoForge.EVENT_BUS.register(com.immortalstorage.immortalstorage.villager.ModTrades.class);', '')
        $datapackSyncSignature = 'public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {'
        $text = $text.Replace($datapackSyncSignature,
            $datapackSyncSignature + $newline +
            '        event.sendRecipes(com.immortalstorage.immortalstorage.recipe.ModRecipes.IMMORTAL_FURNACE_TYPE.get());')
    }
    if ($relativePath -match '[\\/]client[\\/]ClientSetup\.java$') {
        $text = $text.Replace(
            '        ImmortalStorageKeybinds.init(modBus, forgeBus);',
            '        ImmortalStorageKeybinds.init(modBus, forgeBus);' + $newline +
            '        forgeBus.register(com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.class);')
    }
    if ($relativePath -match '[\\/]villager[\\/]ModTrades\.java$') {
        $text = $text.Replace('event.getType() != ModVillagers.IMMORTAL_SAGE.get()',
            'event.getType() != net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "immortal_sage"))')
    }
    if ($relativePath -match '[\\/]villager[\\/]ModVillagers\.java$') {
        $text = $text.Replace('new VillagerProfession("immortal_sage",',
            'new VillagerProfession(net.minecraft.network.chat.Component.translatable("entity.minecraft.villager.immortalstorage.immortal_sage"),')
    }
    if ($relativePath -match '[\\/]worldshard[\\/]WorldShardOreScanner\.java$') {
        $text = $text.Replace(' || type == PlacementModifierType.CARVING_MASK_PLACEMENT', '')
        $text = $text.Replace('registryAccess.registryOrThrow(', 'registryAccess.lookupOrThrow(')
        $text = $text.Replace('stems.getHolder(key).ifPresent(', 'stems.get(key).ifPresent(')
        $text = [regex]::Replace($text,
            'biomeRegistry\.getTag\(([^)]*)\)\.ifPresent\(tag -> tag\.forEach\(fromTag::add\)\);',
            'biomeRegistry.getTagOrEmpty($1).forEach(fromTag::add);')
        $text = [regex]::Replace($text,
            'biomeRegistry\.getTag\(mode\.targetBiomeTag\(\)\.orElseThrow\(\)\)\s*\.ifPresent\(tag -> tag\.forEach\(fromTag::add\)\);',
            'biomeRegistry.getTagOrEmpty(mode.targetBiomeTag().orElseThrow()).forEach(fromTag::add);')
    }

    if ($relativePath -match '[\\/]worldshard[\\/]WorldShardStructureLootScanner\.java$' -or
        $relativePath -match '[\\/]worldshard[\\/]WorldShardLootReloadListener\.java$') {
        # 26.1.2 把 RegistryAccess.registryOrThrow 重命名为 lookupOrThrow；
        # 战利品目录和重载监听器都从 LOOT_TABLE registry 解析真实战利品表。
        $text = $text.Replace('registryAccess.registryOrThrow(', 'registryAccess.lookupOrThrow(')
    }

    if ($relativePath -match '[\\/]worldshard[\\/]WorldShardLootCatalog\.java$') {
        # 26.1.2 的 Registry#get(ResourceKey) 返回 Optional<Holder.Reference<T>> 而非
        # 1.21.1 的 @Nullable T；把战利品表解析映射回可空表引用。
        $text = $text.Replace('lootTables.get(ResourceKey.create(Registries.LOOT_TABLE, id))',
            'lootTables.get(ResourceKey.create(Registries.LOOT_TABLE, id)).map(net.minecraft.core.Holder.Reference::value).orElse(null)')
    }

    if ($relativePath -match '[\\/]dimension[\\/]RealmHelper\.java$') {
        $text = $text.Replace('player.getRespawnDimension()',
            '(player.getRespawnConfig() == null ? null : player.getRespawnConfig().respawnData().dimension())')
        $text = $text.Replace('getRespawnConfig().dimension()',
            'getRespawnConfig().respawnData().dimension()')
        $text = $text.Replace('target.getSharedSpawnPos()',
            'target.getRespawnData().pos()')
        $text = $text.Replace('new net.minecraft.world.level.ChunkPos(pos)',
            'net.minecraft.world.level.ChunkPos.containing(pos)')
        $text = $text.Replace('player.getYRot(), player.getXRot());',
            'player.getYRot(), player.getXRot(), false);')
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/]SubstitutePuppetItem\.java$') {
        $text = [regex]::Replace($text,
            'player\.teleportTo\(targetLevel,\s*pos\.getX\(\) \+ 0\.5D,\s*pos\.getY\(\) \+ 1\.0D,\s*pos\.getZ\(\) \+ 0\.5D,\s*player\.getYRot\(\),\s*player\.getXRot\(\)\);',
            ('player.teleportTo(targetLevel, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,' + $newline +
                '                java.util.Set.of(), player.getYRot(), player.getXRot(), false);'))
    }

    if ($relativePath -match '[\\/]effect[\\/]custom[\\/]LingqiSaturationEffect\.java$') {
        $text = $text.Replace('import net.minecraft.world.entity.LivingEntity;',
            'import net.minecraft.world.entity.LivingEntity;' + $newline +
            'import net.minecraft.server.level.ServerLevel;')
        $text = $text.Replace('applyEffectTick(LivingEntity entity, int amplifier)',
            'applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier)')
    }
    if ($relativePath -match '[\\/]entity[\\/]PrimordialQiConversion\.java$') {
        # SpawnEggItem#getType now reads the entity type directly from the
        # item; it no longer accepts a registry provider in 26.1.2.
        $text = $text.Replace('findRegisteredSpawnEgg(level.registryAccess(), living.getType())',
            'findRegisteredSpawnEgg(living.getType())')
        $text = $text.Replace('static SpawnEggItem findRegisteredSpawnEgg(net.minecraft.core.HolderLookup.Provider registries, EntityType<?> entityType)',
            'static SpawnEggItem findRegisteredSpawnEgg(EntityType<?> entityType)')
        $text = $text.Replace('candidate.getType(registries, candidate.getDefaultInstance())',
            'candidate.getType(candidate.getDefaultInstance())')
    }
    if ($relativePath -match '[\\/]event[\\/]TribulationHelper\.java$') {
        $text = $text.Replace('targetType.create(player.serverLevel())',
            'targetType.create((net.minecraft.server.level.ServerLevel) player.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)')
        $text = $text.Replace('type.create(level)',
            'type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)')
    }
    if ($relativePath -match '[\\/]dimension[\\/]XianqiaoRealmChunkGenerator\.java$') {
        $text = $text.Replace('chunk.setBlockState(m, b, false)', 'chunk.setBlockState(m, b, 0)')
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/]SoulCatcherItem\.java$') {
        $text = $text.Replace('if (!target.save(entityTag))',
            "if (!$script:compatNbt.saveEntity(target, entityTag))")
        $text = $text.Replace('Vec3.atLowerCornerOf(context.getClickedFace().getNormal())',
            'new Vec3(context.getClickedFace().getStepX(), context.getClickedFace().getStepY(), context.getClickedFace().getStepZ())')
        $text = $text.Replace('loaded.moveTo(', 'loaded.snapTo(')
    }
    if ($relativePath -match '[\\/]menu[\\/]custom[\\/]TerminalArmorSlot\.java$') {
        $text = $text.Replace('import com.mojang.datafixers.util.Pair;', '')
        $text = $text.Replace('import net.minecraft.world.inventory.InventoryMenu;', '')
        $text = [regex]::Replace($text,
            '(?s)@Override public Pair<(?:ResourceLocation|Identifier), (?:ResourceLocation|Identifier)> getNoItemIcon\(\)\s*\{\s*return Pair\.of\(InventoryMenu\.BLOCK_ATLAS, emptyIcon\);\s*\}',
            '@Override public ResourceLocation getNoItemIcon() { return emptyIcon; }')
        $text = [regex]::Replace($text,
            '(?s)@Override public Pair<Identifier, Identifier> getNoItemIcon\(\)\s*\{\s*return Pair\.of\(InventoryMenu\.BLOCK_ATLAS, emptyIcon\);\s*\}',
            '@Override public Identifier getNoItemIcon() { return emptyIcon; }')
    }

    # Level#recipeAccess is intentionally typed as the smaller RecipeAccess
    # interface. The preserved implementation still needs the concrete
    # RecipeManager for ordered collection scans, so cast only at this
    # compatibility boundary (the runtime object remains RecipeManager).
    $text = $text.Replace('RecipeManager manager = level.recipeAccess()',
        'RecipeManager manager = (RecipeManager) level.recipeAccess()')
    $text = $text.Replace('RecipeManager manager = player.level().recipeAccess()',
        'RecipeManager manager = (RecipeManager) player.level().recipeAccess()')
    $text = $text.Replace('RecipeManager manager = serverPlayer.level().recipeAccess()',
        'RecipeManager manager = (RecipeManager) serverPlayer.level().recipeAccess()')
    $text = $text.Replace("$script:compatRecipeAccess.getRecipesFor(level.recipeAccess(),",
        "$script:compatRecipeAccess.getRecipesFor((RecipeManager) level.recipeAccess(),")
    $text = $text.Replace("$script:compatRecipeAccess.getRecipesFor(player.level().recipeAccess(),",
        "$script:compatRecipeAccess.getRecipesFor((RecipeManager) player.level().recipeAccess(),")
    $text = $text.Replace("$script:compatRecipeAccess.getRecipesFor(serverPlayer.level().recipeAccess(),",
        "$script:compatRecipeAccess.getRecipesFor((RecipeManager) serverPlayer.level().recipeAccess(),")
    $text = $text.Replace("$script:compatRecipeAccess.getAllRecipesFor(level.recipeAccess(),",
        "$script:compatRecipeAccess.getAllRecipesFor((RecipeManager) level.recipeAccess(),")
    $text = $text.Replace("$script:compatRecipeAccess.getAllRecipesFor(player.level().recipeAccess(),",
        "$script:compatRecipeAccess.getAllRecipesFor((RecipeManager) player.level().recipeAccess(),")
    $text = $text.Replace("$script:compatRecipeAccess.byKey(level.recipeAccess(),", 
        "$script:compatRecipeAccess.byKey((RecipeManager) level.recipeAccess(),")

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]ImmortalFurnaceBlockEntity\.java$' -or
        $relativePath -match '[\\/]menu[\\/]custom[\\/]EmbeddedImmortalFurnaceBackend\.java$' -or
        $relativePath -match '[\\/]compat[\\/]emi[\\/]ImmortalStorageEmiPlugin\.java$' -or
        $relativePath -match '[\\/]compat[\\/]emi[\\/]ImmortalFurnaceEmiRecipe\.java$') {
        $text = $text.Replace('holder.id()', 'holder.id().identifier()')
        $text = $text.Replace('candidate.holder().id()', 'candidate.holder().id().identifier()')
    }
    if ($relativePath -match '[\\/]menu[\\/]custom[\\/]EmbeddedImmortalFurnaceBackend\.java$') {
        # Slot validation runs on both logical sides.  On 26.1.2 the physical
        # client exposes ClientRecipeContainer through RecipeAccess, not the
        # server-only RecipeManager needed by the complete recipe scan below.
        # Keep the server authoritative and let the client accept the cursor
        # stack provisionally; the matching server menu still rejects invalid
        # inputs and synchronizes the resulting slot state.
        $text = [regex]::Replace($text,
            '(?s)(boolean isRecipeInput\(Player player, ItemStack stack\) \{\s*' +
                'if \(player == null \|\| stack == null \|\| stack\.isEmpty\(\)\) return false;\s*)' +
                '(return findRecipe\(player, stack\)\.isPresent\(\);)',
            ('$1if (player.level().isClientSide()) return true;' + $newline +
                '        $2'))
    }
    if ($relativePath -match '[\\/]compat[\\/]emi[\\/]ImmortalFurnaceEmiRecipe\.java$' -or
        $relativePath -match '[\\/]compat[\\/]jei[\\/]ImmortalFurnaceJeiCategory\.java$') {
        $text = $text.Replace('holder.value().getIngredients().getFirst()', 'holder.value().input()')
        $text = $text.Replace('holder.value().getCookingTime()', 'holder.value().cookingTime()')
        $text = $text.Replace('holder.value().getResultItem(Minecraft.getInstance().level.registryAccess())',
            'holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), Minecraft.getInstance().level.registryAccess())')
        $text = $text.Replace('holder.value().getResultItem(net.minecraft.client.Minecraft.getInstance().level.registryAccess())',
            'holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), net.minecraft.client.Minecraft.getInstance().level.registryAccess())')
        $text = [regex]::Replace($text,
            'holder\.value\(\)\.getResultItem\(\s*net\.minecraft\.client\.Minecraft\.getInstance\(\)\.level\.registryAccess\(\)\s*\)',
            'holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), net.minecraft.client.Minecraft.getInstance().level.registryAccess())')
        $text = [regex]::Replace($text,
            'holder\.value\(\)\.getResultItem\(\s*(?:net\.minecraft\.client\.)?Minecraft\.getInstance\(\)\.level\.registryAccess\(\)\s*\)',
            'holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), Minecraft.getInstance().level.registryAccess())')
    }
    if ($relativePath -match '[\\/]compat[\\/]jei[\\/]ImmortalStorageJeiPlugin\.java$') {
        $text = $text.Replace('recipe.value().getIngredients()', 'recipe.value().placementInfo().ingredients()')
        $text = $text.Replace('java.util.Arrays.stream(ingredient.getItems())', 'ingredient.items()')
        $text = $text.Replace('ItemStack::getMaxStackSize', 'stack -> stack.getMaxStackSize()')
        $text = $text.Replace('.mapToInt(stack -> stack.getMaxStackSize())',
            '.mapToInt(holder -> holder.value().getMaxStackSize(new ItemStack(holder.value())))')
        $text = $text.Replace('recipe::isTemplateIngredient',
            'stack -> recipe.templateIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')
        $text = $text.Replace('recipe::isBaseIngredient', 'stack -> recipe.baseIngredient().test(stack)')
        $text = $text.Replace('recipe::isAdditionIngredient',
            'stack -> recipe.additionIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')
    }
    $text = [regex]::Replace($text,
        'holder\.value\(\)\.getResultItem\(\s*(?:net\.minecraft\.client\.)?Minecraft\.getInstance\(\)\.level\.registryAccess\(\)\s*\)',
        'holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), Minecraft.getInstance().level.registryAccess())')
    $text = $text.Replace('holder.value().isTemplateIngredient(stack)',
        'holder.value().templateIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')
    $text = $text.Replace('holder.value().isBaseIngredient(stack)',
        'holder.value().baseIngredient().test(stack)')
    $text = $text.Replace('holder.value().isAdditionIngredient(stack)',
        'holder.value().additionIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')
    $text = $text.Replace('value::isTemplateIngredient',
        'stack -> value.templateIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')
    $text = $text.Replace('value::isBaseIngredient', 'stack -> value.baseIngredient().test(stack)')
    $text = $text.Replace('value::isAdditionIngredient',
        'stack -> value.additionIngredient().map(ingredient -> ingredient.test(stack)).orElse(false)')

    if ($relativePath -match '[\\/]item[\\/]ModItems\.java$') {
        $text = $text.Replace('return props.rarity(rarityFor(name));',
            'return props.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, name))).rarity(rarityFor(name));')
        $text = $text.Replace('return base.food(pillFood(hunger, sat));',
            'return base.food(pillFood(hunger, sat), net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(0.8F).build());')
        $text = $text.Replace('.fast()', '')
    }
    if ($relativePath -match '[\\/]block[\\/]ModBlocks\.java$') {
        $text = $text.Replace('        return props;',
            '        return props.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, name)));')
    }
    if ($relativePath -match '[\\/]block[\\/]custom[\\/]SourceVeinBlock\.java$') {
        $text = $text.Replace('super(idProps(kind.name().toLowerCase(), BlockBehaviour.Properties.of().strength(1.0f).lightLevel(s -> 4).requiresCorrectToolForDrops().noOcclusion()));',
            'super(idProps(genericDefinitionCarrier ? "custom_source_vein" : kind.name().toLowerCase(), BlockBehaviour.Properties.of().strength(1.0f).lightLevel(s -> 4).requiresCorrectToolForDrops().noOcclusion()));')
        $text = $text.Replace('        return p;',
            '        String id = "custom_source_vein".equals(name) ? name : ("cobble".equals(name) ? "cobblestone_vein" : name + "_vein");' + $newline +
            '        return p.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, id)));')
    }
    if ($relativePath -match '[\\/]block[\\/]custom[\\/]YuanLightBlock\.java$') {
        $text = $text.Replace('super(Properties.of().mapColor(MapColor.NONE).noOcclusion().noCollision().instabreak()',
            'super(Properties.of().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(com.immortalstorage.immortalstorage.ImmortalStorageMod.MODID, immortal ? "immortal_yuan_light" : "true_yuan_light"))).mapColor(MapColor.NONE).noOcclusion().noCollision().instabreak()')
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/]ImmortalPillItem\.java$') {
        $text = $text.Replace('.fast()', '')
        $text = [regex]::Replace($text,
            'super\(props\.food\(new FoodProperties\.Builder\(\)\.nutrition\(20\)\.saturationModifier\(20f\)\s*\.alwaysEdible\(\)\.build\(\)\)\);',
            ('super(props.food(new FoodProperties.Builder().nutrition(20).saturationModifier(20f).alwaysEdible().build(),' + $newline +
                '                net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(0.8F).build()));'))
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/](ImmortalYuanItem|TrueYuanItem)\.java$') {
        $text = [regex]::Replace($text,
            '(?m)^\s*@Override public String getDescriptionId\(\) \{[^\r\n]*\}\s*\r?\n?', '')
    }
    if ($relativePath -match '[\\/]compat[\\/]jei[\\/]ImmortalStorageJeiPlugin\.java$') {
        $text = $text.Replace('recipe.id()', 'recipe.id().identifier()')

        # 26.1 keeps only the synchronized RecipeAccess view on the physical
        # client.  ClientLevel#recipeAccess() therefore returns
        # ClientRecipeContainer, not RecipeManager.  The target lane sends
        # the one recipe type used by this JEI category through
        # OnDatapackSyncEvent/RecipesReceivedEvent and owns the client cache in
        # TargetClientRecipeCache.
        $text = [regex]::Replace($text,
            '(?s)if \(level != null\) registration\.addRecipes\(ImmortalFurnaceJeiCategory\.TYPE,\s*' +
                '.*?com\.immortalstorage\.immortalstorage\.recipe\.ModRecipes\.IMMORTAL_FURNACE_TYPE\.get\(\)\)\);',
            'registration.addRecipes(ImmortalFurnaceJeiCategory.TYPE,' + $newline +
                '                com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.immortalFurnaceRecipes());')
        $text = [regex]::Replace($text,
            '(?m)^\s*var level = net\.minecraft\.client\.Minecraft\.getInstance\(\)\.level;\r?\n', '')
        $text = [regex]::Replace($text,
            '(?m)^(\s*)runtime = jeiRuntime;\r?$',
            { param($match) $match.Groups[1].Value + 'runtime = jeiRuntime;' + $newline +
                $match.Groups[1].Value + 'com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.bindJei(ImmortalStorageJeiPlugin::refreshImmortalFurnaceRecipes);' })
        $text = [regex]::Replace($text,
            '(?m)^(\s*)runtime = null;\r?$',
            { param($match) $match.Groups[1].Value + 'com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.unbindJei();' + $newline +
                $match.Groups[1].Value + 'runtime = null;' })
        $text = [regex]::Replace($text,
            '(?m)^\s*@Override\s*\r?\n\s*public void registerRecipeCatalysts',
            ('    public static void refreshImmortalFurnaceRecipes(' +
                'java.util.List<net.minecraft.world.item.crafting.RecipeHolder<' +
                'net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipes) {' + $newline +
                '        if (runtime != null) runtime.getRecipeManager().addRecipes(' +
                'ImmortalFurnaceJeiCategory.TYPE, recipes);' + $newline +
                '    }' + $newline + $newline +
                '    @Override' + $newline +
                '    public void registerRecipeCatalysts'))
        $text = [regex]::Replace($text,
            '(?m)^import net\.minecraft\.world\.item\.crafting\.RecipeManager;\r?\n', '')
    }

    # Compound-backed stack persistence now uses the registry-aware codecs.
    $text = $text.Replace('ItemStack.parseOptional(', "$script:compatCodec.parseItemStack(")
    $text = $text.Replace('FluidStack.parseOptional(', "$script:compatCodec.parseFluidStack(")
    $text = $text.Replace('codecSafe.saveOptional(registryAccess)',
        "$script:compatCodec.saveItemStack(registryAccess, codecSafe)")
    # The canonical `prototype` in ImmortalStoragePlayerData is a FluidStack;
    # keep the registry-aware fluid codec instead of selecting the ItemStack
    # overload during source migration.
    $text = $text.Replace('prototype.saveOptional(registryAccess)',
        "$script:compatCodec.saveFluidStack(registryAccess, prototype)")
    $text = $text.Replace('prototype.saveOptional(registries)',
        "$script:compatCodec.saveFluidStack(registries, prototype)")
    $text = $text.Replace('resource.fluidKey.prototype().saveOptional(registries)',
        "$script:compatCodec.saveFluidStack(registries, resource.fluidKey.prototype())")
    $text = $text.Replace('resource.itemKey.prototype().copyWithCount(1).saveOptional(registries)',
        "$script:compatCodec.saveItemStack(registries, resource.itemKey.prototype().copyWithCount(1))")
    $text = $text.Replace('fluid.copyWithAmount(1).saveOptional(blockEntity.getLevel().registryAccess())',
        "$script:compatCodec.saveFluidStack(blockEntity.getLevel().registryAccess(), fluid.copyWithAmount(1))")
    $text = $text.Replace('filters.get(slot).save(registries)',
        "$script:compatCodec.saveItemStack(registries, filters.get(slot))")
    $text = $text.Replace('reinforcementPlugin.save(registries)',
        "$script:compatCodec.saveItemStack(registries, reinforcementPlugin)")
    $text = $text.Replace('plugin.save(registries)',
        "$script:compatCodec.saveItemStack(registries, plugin)")
    $text = $text.Replace('legacyPluginOverflow.save(registries)',
        "$script:compatCodec.saveItemStack(registries, legacyPluginOverflow)")
    $text = $text.Replace('stack.save(registries)',
        "$script:compatCodec.saveItemStack(registries, stack)")
    $text = [regex]::Replace($text, '(\b\w+)\.serializeNBT\(registries\)',
        "$script:compatValueIo.serialize(`$1, registries)")
    $text = [regex]::Replace($text, '(\b\w+)\.deserializeNBT\(registries,',
        "$script:compatValueIo.deserialize(`$1, registries,")

    # Player inventory equipment moved behind Player's equipment accessors.
    $text = $text.Replace('owner.getInventory().items',
        "$script:compatPlayerInventory.items(owner)")
    $text = $text.Replace('owner.getInventory().offhand',
        "$script:compatPlayerInventory.slot(owner, net.minecraft.world.entity.EquipmentSlot.OFFHAND)")
    $text = $text.Replace('owner.getInventory().armor',
        "$script:compatPlayerInventory.armor(owner)")

    $text = $text.Replace('import net.minecraft.world.entity.RelativeMovement;',
        'import net.minecraft.world.entity.Relative;')
    $text = $text.Replace('RelativeMovement', 'Relative')
    $text = $text.Replace('net.minecraft.world.level.block.FarmBlock',
        'net.minecraft.world.level.block.FarmlandBlock')
    $text = $text.Replace('BlockEvent.BreakEvent', 'BreakBlockEvent')
    if ($text.Contains('BreakBlockEvent') -and
        -not $text.Contains('import net.neoforged.neoforge.event.level.block.BreakBlockEvent;')) {
        $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
            param($match) $match.Value + $newline +
                'import net.neoforged.neoforge.event.level.block.BreakBlockEvent;'
        }, 1)
    }
    $text = $text.Replace('ChunkAccess c, GenerationStep.Carving carving', 'ChunkAccess c')
    $text = $text.Replace('protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos)',
        'protected int getLightBlock(BlockState state)')
    $text = $text.Replace('protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)',
        'protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction)')
    $text = $text.Replace('FarmBlock.MOISTURE', 'FarmlandBlock.MOISTURE')
    $text = $text.Replace('state.getLightBlock()', 'state.getLightDampening()')
    $text = $text.Replace('state.getLightBlock(level, cursor)', 'state.getLightDampening()')
    $text = $text.Replace('new RandomSequences(seed)', 'new RandomSequences()')
    $text = $text.Replace('minecraft.level.random', 'minecraft.level.getRandom()')
    $text = $text.Replace('event.getCamera().getPosition()', 'event.getCamera().position()')
    $text = $text.Replace('new net.minecraft.world.level.ChunkPos(pos)',
        'net.minecraft.world.level.ChunkPos.containing(pos)')
    $text = $text.Replace('DyeItem dye = (DyeItem)', 'DyeItem dye = (DyeItem)')
    $text = $text.Replace('dye.getDyeColor()',
        'stack.getOrDefault(net.minecraft.core.component.DataComponents.DYE, net.minecraft.world.item.DyeColor.WHITE)')
    $text = $text.Replace('BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(', 'BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(')
    $text = $text.Replace('BlockEntityType.getKey(', 'BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(')
    if ($text.Contains('BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(') -and
        -not $text.Contains('import net.minecraft.core.registries.BuiltInRegistries;')) {
        $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
            param($match) $match.Value + $newline +
                'import net.minecraft.core.registries.BuiltInRegistries;'
        }, 1)
    }

    if ($relativePath -match '[\\/]dimension[\\/]PersonalRealmLevelData\.java$') {
        $text = $text.Replace('net.minecraft.world.level.storage.DimensionDataStorage',
            'net.minecraft.world.level.storage.SavedDataStorage')
        $text = $text.Replace('void bindPersistence(DimensionDataStorage storage)',
            'void bindPersistence(SavedDataStorage storage)')
        $text = $text.Replace('new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS)',
            'new TimerQueue<>()')
        $text = $text.Replace('private static final String CLOCK_DATA_ID = "immortalstorage_personal_realm_clock";',
            'private static final net.minecraft.resources.Identifier CLOCK_DATA_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath("immortalstorage", "personal_realm_clock");')
        $text = $text.Replace('import net.minecraft.world.level.storage.DimensionDataStorage;',
            'import net.minecraft.world.level.storage.SavedDataStorage;')
        $text = $text.Replace('    @Override', '')
    }

    if ($relativePath -match '[\\/]dimension[\\/]PersonalRealmLevelFactory\.java$') {
        $text = [regex]::Replace($text,
            '(?m)^import net\.minecraft\.server\.level\.progress\.ChunkProgressListener;\r?\n', '')
        $text = [regex]::Replace($text,
            '(?m)^import net\.minecraft\.world\.RandomSequences;\r?\n', '')
        $text = [regex]::Replace($text,
            '(?s)\s*private static final ChunkProgressListener NOOP_PROGRESS = new ChunkProgressListener\(\) \{.*?\n\s*\};\r?\n',
            '')
        $text = $text.Replace('worldData.worldGenOptions().seed()', 'server.overworld().getSeed()')
        $text = [regex]::Replace($text,
            '(?m)^\s*RandomSequences randomSequences = new RandomSequences\(\);\r?\n', '')
        $text = [regex]::Replace($text, '(?m)^\s*NOOP_PROGRESS,\r?\n', '')
        $text = [regex]::Replace($text, '(?m)^\s*randomSequences,\r?\n', '')
    }

    if ($relativePath -match '[\\/]dimension[\\/]PersonalRealmServerLevel\.java$') {
        $text = [regex]::Replace($text,
            '(?m)^import net\.minecraft\.server\.level\.progress\.ChunkProgressListener;\r?\n', '')
        $text = [regex]::Replace($text,
            '(?m)^import net\.minecraft\.world\.RandomSequences;\r?\n', '')
        $text = [regex]::Replace($text,
            '(?m)^\s*ChunkProgressListener progressListener,\r?\n', '')
        $text = [regex]::Replace($text,
            '(?m)^\s*@Nullable RandomSequences randomSequences,\r?\n', '')
        $text = [regex]::Replace($text,
            'stem,\s*progressListener,\s*debug, seed, customSpawners, tickTime, randomSequences\)',
            'stem, debug, seed, customSpawners, tickTime)')
        $text = $text.Replace('setDayTime(RealmEnvironmentPolicy.lockedDayTime(realmLevelData.lockedDaytime()));',
            'realmLevelData.setDayTime(RealmEnvironmentPolicy.lockedDayTime(realmLevelData.lockedDaytime()));')
    }

    # ServerPlayer no longer exposes the old serverLevel() convenience method.
    # Keep the server-only contract explicit at call sites instead of changing
    # the surrounding gameplay logic to a client-capable Level path.
    foreach ($playerVariable in @('player', 'serverPlayer', 'ownerPlayer', 'killer')) {
        $text = $text.Replace("$playerVariable.serverLevel()",
            "(net.minecraft.server.level.ServerLevel) $playerVariable.level()")
    }
    $text = [regex]::Replace($text,
        '\(net\.minecraft\.server\.level\.ServerLevel\)\s+(\w+)\.level\(\)\.',
        { param($match) "((net.minecraft.server.level.ServerLevel) $($match.Groups[1].Value).level())." })

    # ServerPlayer.server became an accessor. Restrict this rewrite to known
    # player variables so unrelated MinecraftServer locals keep their names.
    $text = [regex]::Replace($text,
        '(?<![\w.])(player|serverPlayer|ownerPlayer|owner|killer|p|sp)\.server\b',
        { param($match) "$script:compatLevel.server($($match.Groups[1].Value).level())" })
    $text = [regex]::Replace($text,
        '(?<![\w.])(player|serverPlayer|ownerPlayer|owner|killer|p|sp)\.getServer\(\)\.(?:getRecipeManager|recipeAccess)\(\)',
        { param($match) "(net.minecraft.world.item.crafting.RecipeManager) $($match.Groups[1].Value).level().recipeAccess()" })

    # Inventory exposes views instead of mutable public lists in 26.1.2.
    $text = [regex]::Replace($text,
        '(\w+)\.getInventory\(\)\.items',
        { param($match) "$script:compatPlayerInventory.items($($match.Groups[1].Value))" })
    $text = [regex]::Replace($text,
        '(\w+)\.getInventory\(\)\.offhand',
        { param($match) "$script:compatPlayerInventory.slot($($match.Groups[1].Value), net.minecraft.world.entity.EquipmentSlot.OFFHAND)" })
    $text = [regex]::Replace($text,
        '(\w+)\.getInventory\(\)\.armor',
        { param($match) "$script:compatPlayerInventory.armor($($match.Groups[1].Value))" })

    # Cooldowns are keyed by the stack so component-specific cooldown groups
    # remain intact across the migration.
    $text = $text.Replace('context.getItemInHand().getItem(), 4', 'context.getItemInHand(), 4')
    $text = $text.Replace('staff.getItem(), 4', 'staff, 4')

    # Item crafting hooks no longer receive a Level. Keep the original stack
    # count and let ItemStack dispatch the target-version callback.
    $text = [regex]::Replace($text,
        '(?<![\w.])(\w+)\.getItem\(\)\.onCraftedBy\((\w+),\s*(\w+)\.level\(\),\s*(\w+)\)',
        { param($match) "$($match.Groups[2].Value).onCraftedBy($($match.Groups[4].Value), $($match.Groups[2].Value).getCount())" })
    $text = [regex]::Replace($text,
        '(?<![\w.])(\w+)\.onCraftedBy\((\w+)\.level\(\),\s*(\w+),\s*([^)]+)\)',
        { param($match) "$($match.Groups[1].Value).onCraftedBy($($match.Groups[3].Value), $($match.Groups[4].Value))" })
    $text = [regex]::Replace($text,
        '([\w.]+)\.setRecipeUsed\(\w+\.level\(\),\s*(\w+),\s*([^)]+)\)',
        { param($match) "$($match.Groups[1].Value).setRecipeUsed($($match.Groups[2].Value), $($match.Groups[3].Value))" })

    # A cast must wrap the RecipeAccess receiver before its method call.
    $text = [regex]::Replace($text,
        '\(net\.minecraft\.world\.item\.crafting\.RecipeManager\)\s+(\w+)\.level\(\)\.recipeAccess\(\)\s*\.',
        { param($match) '((net.minecraft.world.item.crafting.RecipeManager) ' + $match.Groups[1].Value + '.level().recipeAccess()).' })

    # Target MobEffects renamed the public singleton holders.
    $text = $text.Replace('MobEffects.DIG_SPEED', 'MobEffects.HASTE')
    $text = $text.Replace('MobEffects.MOVEMENT_SPEED', 'MobEffects.SPEED')
    $text = $text.Replace('MobEffects.DAMAGE_BOOST', 'MobEffects.STRENGTH')
    $text = $text.Replace('MobEffects.DAMAGE_RESISTANCE', 'MobEffects.RESISTANCE')
    $text = $text.Replace('MobEffects.MOVEMENT_SLOWDOWN', 'MobEffects.SLOWNESS')

    if ($text.Contains('(RecipeManager)') -and
        -not $text.Contains('import net.minecraft.world.item.crafting.RecipeManager;')) {
        $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
            param($match) $match.Value + $newline + 'import net.minecraft.world.item.crafting.RecipeManager;'
        }, 1)
    }

    # EntityType loading now records the reason for a deserialized entity.
    $text = $text.Replace('EntityType.loadEntityRecursive(entityTag, level, loaded -> {',
        'EntityType.loadEntityRecursive(entityTag, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> {')
    $text = $text.Replace('EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), level, loaded -> loaded)',
        'EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), level, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> loaded)')
    $text = $text.Replace('EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), displayLevel, loaded -> loaded)',
        'EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), displayLevel, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> loaded)')
    $text = $text.Replace('.create(level)',
        '.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE)')
    $text = $text.Replace('.create(displayLevel)',
        '.create(displayLevel, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE)')

    if ($relativePath -match '[\\/]item[\\/]custom[\\/]SpiritSwordItem\.java$') {
        $text = $text.Replace('import net.minecraft.world.item.SwordItem;', '')
        $text = $text.Replace('extends SwordItem', "extends $script:compatSwordItem")
        $text = [regex]::Replace($text,
            'super\(ModItemsTierAccess\.SPIRIT_MATERIAL,\s*props\.fireResistant\(\)\.durability\(2500\)\s*\.attributes\(SwordItem\.createAttributes\(ModItemsTierAccess\.SPIRIT_MATERIAL,\s*0\.0F,\s*-2\.4F\)\)\);',
            'super(props.fireResistant().durability(2500).sword(ModItemsTierAccess.SPIRIT_MATERIAL, 0.0F, -2.4F));')
        $text = $text.Replace('public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)',
            'public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)')
        $text = $text.Replace('return super.hurtEnemy(stack, target, attacker);',
            'super.hurtEnemy(stack, target, attacker);')
    }

    if ($relativePath -match '[\\/]client[\\/]ClientSetup\.java$') {
        # 26.1 uses the model's render pipeline/material metadata for block
        # transparency; the removed ItemBlockRenderTypes global layer hook is
        # deliberately not carried into the target source tree.
        $text = [regex]::Replace($text,
            '(?s)\s*net\.minecraft\.client\.renderer\.ItemBlockRenderTypes\.setRenderLayer\(\s*ModBlocks\.ENERGY_CRYSTAL\.get\(\),\s*net\.minecraft\.client\.renderer\.rendertype\.RenderType\.translucent\(\)\);',
            '')
        $text = [regex]::Replace($text,
            '(?s)\s*ItemBlockRenderTypes\.setRenderLayer\(\s*ModBlocks\.ENERGY_CRYSTAL\.get\(\),\s*(?:net\.minecraft\.client\.renderer\.)?RenderType\.translucent\(\)\);',
            '')
        # Optional mana/source variants use the same model material and must
        # follow the same 26.1 material pipeline.  Remove their legacy global
        # layer calls as well; otherwise a target build fails only when the
        # optional registry entries are present in the canonical source.
        $text = [regex]::Replace($text,
            '(?s)\s*if\s*\(ModBlocks\.MANA_CRYSTAL\s*!=\s*null\)\s*\{.*?ItemBlockRenderTypes\.setRenderLayer\(.*?\);\s*\}',
            '')
        $text = [regex]::Replace($text,
            '(?s)\s*if\s*\(ModBlocks\.SOURCE_CRYSTAL\s*!=\s*null\)\s*\{.*?ItemBlockRenderTypes\.setRenderLayer\(.*?\);\s*\}',
            '')
        # World barrier follows the same 26.1 model-material pipeline; drop the
        # removed global layer hook and rely on the block model's render_type.
        $text = [regex]::Replace($text,
            '(?s)\s*net\.minecraft\.client\.renderer\.ItemBlockRenderTypes\.setRenderLayer\(\s*ModBlocks\.WORLD_BARRIER\.get\(\),\s*net\.minecraft\.client\.renderer\.rendertype\.RenderType\.translucent\(\)\);',
            '')
        $text = [regex]::Replace($text,
            '(?s)\s*ItemBlockRenderTypes\.setRenderLayer\(\s*ModBlocks\.WORLD_BARRIER\.get\(\),\s*(?:net\.minecraft\.client\.renderer\.)?RenderType\.translucent\(\)\);',
            '')
        $text = $text.Replace('import net.minecraft.client.renderer.item.ItemProperties;', '')
        $text = $text.Replace('import net.neoforged.neoforge.client.event.ModelEvent;',
            'import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;')
        $text = $text.Replace('modBus.addListener(ClientSetup::registerAdditionalModels);',
            'modBus.addListener(ClientSetup::registerSpecialModelRenderers);')
        $text = [regex]::Replace($text,
            '(?s)\s*ItemProperties\.register\(.*?\);\s*com\.immortalstorage\.immortalstorage\.compat\.CompatManager\.initializeClientIntegrations\(\);',
            $newline + '            com.immortalstorage.immortalstorage.compat.CompatManager.initializeClientIntegrations();')
        $text = [regex]::Replace($text,
            '(?s)\r?\n    private static void registerAdditionalModels\(ModelEvent\.RegisterAdditional event\) \{.*?\r?\n    \}\r?\n\r?\n    private static void registerItemDecorations',
            $newline + '    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {' + $newline +
            '        event.register(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "dynamic_preview"),' + $newline +
            '                com.immortalstorage.immortalstorage.client.render.DynamicPreviewBlockItemRenderer.Unbaked.MAP_CODEC);' + $newline +
            '        event.register(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "source_vein"),' + $newline +
            '                com.immortalstorage.immortalstorage.client.render.SourceVeinItemRenderer.Unbaked.MAP_CODEC);' + $newline +
            '        event.register(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "source_vein_manager"),' + $newline +
            '                com.immortalstorage.immortalstorage.client.render.SourceVeinManagerItemRenderer.Unbaked.MAP_CODEC);' + $newline +
            '    }' + $newline + $newline +
            '    private static void registerItemDecorations')
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/]ImmortalRuinForgedSpiritSwordItem\.java$') {
        $text = $text.Replace('public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)',
            'public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)')
        $text = $text.Replace('boolean result = super.hurtEnemy(stack, target, attacker);',
            'super.hurtEnemy(stack, target, attacker);')
        $text = $text.Replace('        return result;', '')
    }
    if ($relativePath -match '[\\/]item[\\/]weapon[\\/]ModWeaponAttackProjection\.java$') {
        $text = $text.Replace('import net.minecraft.world.item.SwordItem;', '')
        $text = $text.Replace('ToolMaterial tier', 'ToolMaterial material')
        $text = $text.Replace('tier.getAttackDamageBonus()', 'material.attackDamageBonus()')
        $text = $text.Replace('SwordItem.createAttributes(tier, itemBonus, attackSpeed)',
            "$script:compatAttributes.swordAttributes(material, itemBonus, attackSpeed)")
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/]ModItemsTierAccess\.java$') {
        $text = $text.Replace('import net.minecraft.world.item.Tier;', 'import net.minecraft.world.item.ToolMaterial;')
        $text = $text.Replace('import net.minecraft.world.item.Tiers;', '')
        $text = $text.Replace('Tier ', 'ToolMaterial ')
        $text = $text.Replace('Tiers.NETHERITE', 'ToolMaterial.NETHERITE')
    }

    if ($relativePath -match '[\\/]dimension[\\/]PersonalRealmLevelData\.java$') {
        $text = $text.Replace('import net.minecraft.world.level.saveddata.SavedData;',
            ('import net.minecraft.world.level.saveddata.SavedData;' + $newline +
                'import net.minecraft.world.level.saveddata.SavedDataType;' + $newline +
                'import com.mojang.serialization.Codec;' + $newline +
                'import com.mojang.serialization.codecs.RecordCodecBuilder;'))
        $text = $text.Replace('storage.computeIfAbsent(ClockSavedData.FACTORY, CLOCK_DATA_ID)',
            'storage.computeIfAbsent(ClockSavedData.TYPE)')
        $text = [regex]::Replace($text,
            '(?s)private static final SavedData\.Factory<ClockSavedData> FACTORY\s*=\s*\r?\n\s*new SavedData\.Factory<>\(ClockSavedData::new, ClockSavedData::load\);',
            'private static final SavedDataType<ClockSavedData> TYPE = new SavedDataType<>(CLOCK_DATA_ID, ClockSavedData::new, ClockSavedData.CODEC);')
        $clockCodec = @'
        private static final Codec<ClockSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("initialized", false).forGetter(data -> data.initialized),
                Codec.LONG.optionalFieldOf("gameTime", 0L).forGetter(data -> data.gameTime),
                Codec.LONG.optionalFieldOf("dayTime", 0L).forGetter(data -> data.dayTime),
                Codec.BOOL.optionalFieldOf("lockedDaytime", true).forGetter(data -> data.lockedDaytime),
                Codec.INT.optionalFieldOf("lockedWeatherMode", RealmEnvironmentPolicy.CLEAR)
                        .forGetter(data -> data.lockedWeatherMode))
                .apply(instance, (initialized, gameTime, dayTime, lockedDaytime, lockedWeatherMode) -> {
                    ClockSavedData data = new ClockSavedData();
                    data.initialized = initialized;
                    data.gameTime = Math.max(0L, gameTime);
                    data.dayTime = Math.max(0L, dayTime);
                    data.lockedDaytime = lockedDaytime;
                    data.lockedWeatherMode = RealmEnvironmentPolicy.sanitizeWeatherMode(lockedWeatherMode);
                    return data;
                }));
        }
'@
        $text = [regex]::Replace($text,
            '(?s)\r?\n\s*@Override\r?\n\s*public CompoundTag save\(CompoundTag tag, HolderLookup\.Provider registries\) \{.*?\r?\n\s*}\r?\n\s*}',
            $clockCodec)
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]ImmortalFurnaceBlockEntity\.java$') {
        $text = $text.Replace('import net.minecraft.nbt.CompoundTag;',
            ('import net.minecraft.nbt.CompoundTag;' + $newline +
                'import net.minecraft.core.RegistryAccess;' + $newline +
                'import net.minecraft.world.level.block.entity.FuelValues;' + $newline +
                'import net.minecraft.world.level.storage.ValueInput;' + $newline +
                'import net.minecraft.world.level.storage.ValueOutput;'))
        $text = $text.Replace('protected int getBurnDuration(ItemStack stack)',
            'protected int getBurnDuration(FuelValues fuelValues, ItemStack stack)')
        $text = $text.Replace('recordCompletedRecipes(level.recipeAccess())',
            'recordCompletedRecipes((RecipeManager) level.recipeAccess())')
        $text = $text.Replace('super.saveAdditionalLegacy(tag, registries);', '')
        $text = $text.Replace('super.loadAdditionalLegacy(tag, registries);', '')
        $text = [regex]::Replace($text,
            '@Override\s+protected void saveAdditionalLegacy\(CompoundTag tag, HolderLookup\.Provider registries\)',
            'private void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries)')
        $text = [regex]::Replace($text,
            '@Override\s+protected void loadAdditionalLegacy\(CompoundTag tag, HolderLookup\.Provider registries\)',
            'private void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries)')
        $wrappers = @'

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag legacy = new CompoundTag();
        saveAdditionalLegacy(legacy, level != null ? level.registryAccess() : RegistryAccess.EMPTY);
        output.store(legacy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag legacy = input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC))
                .orElseGet(CompoundTag::new);
        loadAdditionalLegacy(legacy, input.lookup());
    }
'@
        $lastBrace = $text.LastIndexOf('}')
        $text = $text.Insert($lastBrace, $wrappers)
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]XianqiaoInterfaceBlockEntity\.java$') {
        $text = $text.Replace('import net.minecraft.nbt.CompoundTag;',
            ('import net.minecraft.nbt.CompoundTag;' + $newline +
                'import net.minecraft.world.level.storage.ValueOutput;'))
        $xianqiaoReplacement = '    @Override' + $newline +
            '    public void removeComponentsFromTag(ValueOutput output) {' + $newline +
            '        super.removeComponentsFromTag(output);' + $newline +
            '        output.discard(BUFFERS_TAG);' + $newline +
            '        if (!preserveRetainedItemBuffersInDrop) {' + $newline +
            '            net.minecraft.nbt.CompoundTag tag = com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.rawOutput(output);' + $newline +
            '            for (net.minecraft.nbt.Tag element : tag.getListOrEmpty(ITEM_SLOTS_TAG)) {' + $newline +
            '                if (element instanceof net.minecraft.nbt.CompoundTag slot) slot.putLong("Cached", 0L);' + $newline +
            '            }' + $newline +
            '        }' + $newline +
            '        if (releaseState != ReleaseState.RELEASED) {' + $newline +
            '            net.minecraft.nbt.CompoundTag tag = com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.rawOutput(output);' + $newline +
            '            for (net.minecraft.nbt.Tag element : tag.getListOrEmpty("ExternalResourceSlots")) {' + $newline +
            '                if (element instanceof net.minecraft.nbt.CompoundTag slot) slot.putLong("Cached", 0L);' + $newline +
            '            }' + $newline +
            '        }' + $newline +
            '    }' + $newline + $newline +
            '    private boolean hasLiveStorage'
        $text = [regex]::Replace($text,
            '(?s)    @Override\s+public void removeComponentsFromTag\(CompoundTag tag\)\s*\{.*?\r?\n    \}\r?\n\r?\n    private boolean hasLiveStorage',
            $xianqiaoReplacement, 1)
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]SimulatedSpiritFieldBlockEntity\.java$') {
        $text = $text.Replace('ContainerHelper.saveAllItems(tag, items, registries)',
            "$script:compatValueIo.saveItems(tag, items, registries)")
        $text = $text.Replace('ContainerHelper.loadAllItems(tag, items, registries)',
            "$script:compatValueIo.loadItems(tag, items, registries)")
        $text = $text.Replace('ContainerHelper.loadAllItems(tag, loaded, registries)',
            "$script:compatValueIo.loadItems(tag, loaded, registries)")
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]EnergyCrystalBlockEntity\.java$') {
        $text = $text.Replace('ContainerHelper.saveAllItems(tag, items, registries)',
            "$script:compatValueIo.saveItems(tag, items, registries)")
        $text = $text.Replace('ContainerHelper.loadAllItems(tag, items, registries)',
            "$script:compatValueIo.loadItems(tag, items, registries)")
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]ModBlockEntities\.java$') {
        $text = $text.Replace('EnergyCrystalBlockEntity::getEnergyHandler',
            '(be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energy(be.getEnergyHandler(side))')
    }

    if ($relativePath -match '[\\/]compat[\\/]EnergyCrystalItemAccess\.java$') {
        $text = $text.Replace('stack.getCapability(Capabilities.Energy.ITEM, null)',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatItemCapabilities.energy(stack)')
    }

    if ($relativePath -match '[\\/]client[\\/]screen[\\/]EnergyCrystalScreen\.java$') {
        $text = $text.Replace('graphics.renderTooltip(font, List.of(',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, font, List.of(')
        $text = $text.Replace('), Optional.empty(), mouseX, mouseY);', '), mouseX, mouseY);')
    }

    if ($relativePath -match '[\\/]player[\\/]ImmortalStoragePlayerData\.java$') {
        $text = $text.Replace(
            'implements com.immortalstorage.immortalstorage.compat.mc2612.LegacyNbtSerializable<CompoundTag>',
            'implements com.immortalstorage.immortalstorage.compat.mc2612.LegacyNbtSerializable<CompoundTag>, net.neoforged.neoforge.common.util.ValueIOSerializable')
        $valueIoMethods = @'

    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        output.store("data", net.minecraft.nbt.CompoundTag.CODEC,
                serializeNBT(net.minecraft.core.RegistryAccess.EMPTY));
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        input.read("data", net.minecraft.nbt.CompoundTag.CODEC)
                .ifPresent(tag -> deserializeNBT(input.lookup(), tag));
    }
'@
        $lastBrace = $text.LastIndexOf('}')
        $text = $text.Insert($lastBrace, $valueIoMethods)
    }

    if ($relativePath -match '[\\/]dimension[\\/]PersonalRealmLevelData\.java$') {
        $text = $text.Replace('this.dayTime = wrapped.getOverworldClockTime();',
            'this.dayTime = 0L;')
        $text = $text.Replace('        private static final SavedDataType<ClockSavedData> TYPE = new SavedDataType<>(CLOCK_DATA_ID, ClockSavedData::new, ClockSavedData.CODEC);',
            '        private static final Codec<ClockSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(' + $newline +
                '                Codec.BOOL.optionalFieldOf("initialized", false).forGetter(data -> data.initialized),' + $newline +
                '                Codec.LONG.optionalFieldOf("gameTime", 0L).forGetter(data -> data.gameTime),' + $newline +
                '                Codec.LONG.optionalFieldOf("dayTime", 0L).forGetter(data -> data.dayTime),' + $newline +
                '                Codec.BOOL.optionalFieldOf("lockedDaytime", true).forGetter(data -> data.lockedDaytime),' + $newline +
                '                Codec.INT.optionalFieldOf("lockedWeatherMode", RealmEnvironmentPolicy.CLEAR).forGetter(data -> data.lockedWeatherMode)' + $newline +
                '        ).apply(instance, (initialized, gameTime, dayTime, lockedDaytime, lockedWeatherMode) -> {' + $newline +
                '            ClockSavedData data = new ClockSavedData();' + $newline +
                '            data.initialized = initialized;' + $newline +
                '            data.gameTime = Math.max(0L, gameTime);' + $newline +
                '            data.dayTime = Math.max(0L, dayTime);' + $newline +
                '            data.lockedDaytime = lockedDaytime;' + $newline +
                '            data.lockedWeatherMode = RealmEnvironmentPolicy.sanitizeWeatherMode(lockedWeatherMode);' + $newline +
                '            return data;' + $newline +
                '        }));' + $newline +
                '        private static final SavedDataType<ClockSavedData> TYPE = new SavedDataType<>(CLOCK_DATA_ID, ClockSavedData::new, CODEC);')
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]SimulatedReincarnationFurnaceBlockEntity\.java$') {
        $text = $text.Replace('egg.getType(level.registryAccess(), source)', 'egg.getType(source)')
        $text = $text.Replace('egg.getType(displayLevel.registryAccess(), source)', 'egg.getType(displayLevel)')
        $text = $text.Replace('specimen.getLootTable()', 'specimen.getLootTable().orElseThrow()')
    }

    if ($relativePath -match '[\\/]block[\\/]custom[\\/]TreasureBasinBlock\.java$') {
        $text = $text.Replace('protected int getLightBlock(BlockState state)',
            'protected int getLightDampening(BlockState state)')
    }

    if ($relativePath -match '[\\/]block[\\/]custom[\\/]SimulatedReincarnationFurnaceBlock\.java$') {
        $text = [regex]::Replace($text,
            '(?s)ItemStack tool = builder\.getOptionalParameter\(LootContextParams\.TOOL\);\s*if \(tool == null \|\| tool\.isEmpty\(\)\) return java\.util\.List\.of\(\);',
            'net.minecraft.world.item.ItemInstance tool = builder.getOptionalParameter(LootContextParams.TOOL);' + $newline +
                '        if (!(tool instanceof ItemStack stack) || stack.isEmpty()) return java.util.List.of();')
        $text = $text.Replace('return tool.getEnchantmentLevel(silk) > 0',
            'return stack.getEnchantmentLevel(silk) > 0')
    }

    # NeoForge's loot modifier base now keeps a deterministic modifier
    # priority. These modifiers have no ordering requirement, so preserve
    # their behavior with the documented neutral priority.
    if ($relativePath -match '[\\/]loot[\\/]custom[\\/](AddItemModifier|ArchaeologyJadeModifier)\.java$') {
        $text = [regex]::Replace($text,
            'super\((conditions\.toArray\([^;]+\))\);',
            'super($1, 0);')
        $text = [regex]::Replace($text,
            'super\(conditions\.toArray\(LootItemCondition\[\]::new\)\);',
            'super(conditions.toArray(LootItemCondition[]::new), 0);')
    }

    # Recipe#assemble no longer carries HolderLookup.Provider in 26.1.2;
    # catch remaining local-variable forms after the input-specific rules.
    $text = [regex]::Replace($text,
        '(\.assemble\([^,()\r\n]+),\s*[^()\r\n]+\.registryAccess\(\)\)',
        '$1)')
    $text = $text.Replace('.assemble(input, player.level().registryAccess())', '.assemble(input)')
    $text = $text.Replace('.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY), net.minecraft.client.Minecraft.getInstance().level.registryAccess())',
        '.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY))')

    # Give all custom item classes the bridge callbacks. Keep vanilla Item::new
    # registrations untouched.
    if ($relativePath -match '[\\/]item[\\/]custom[\\/].*\.java$') {
        $text = [regex]::Replace($text,
            '(\bclass\s+\w+(?:\s*<[^{}]*>)?\s+extends\s+)BlockItem\b',
            "`$1$script:compatBlockItem")
        $text = [regex]::Replace($text,
            '(\bclass\s+\w+(?:\s*<[^{}]*>)?\s+extends\s+)Item\b',
            "`$1$script:compatItem")
    }
    if ($relativePath -match '[\\/]item[\\/]SimpleJadeGuideItem\.java$') {
        $text = [regex]::Replace($text,
            '(\bclass\s+\w+(?:\s*<[^{}]*>)?\s+extends\s+)Item\b',
            "`$1$script:compatItem")
    }

    # ItemStack no longer exposes the old remainder convenience methods.
    $text = $text.Replace('stack.hasCraftingRemainingItem()',
        '!stack.getItem().getCraftingRemainder(stack).isEmpty()')
    $text = $text.Replace('stack.getCraftingRemainingItem()',
        'stack.getItem().getCraftingRemainder(stack)')

    # 26.1.2 replaced Block#onRemove with the server-only removal hook. The
    # hook is only called after the block has been removed, so the old next
    # state comparison is made deterministically true by an air sentinel.
    $removalReplacement = 'protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {' +
        [Environment]::NewLine +
        '        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();'
    $text = [regex]::Replace($text,
        '(?s)protected\s+void\s+onRemove\(BlockState\s+state,\s*Level\s+level,\s*BlockPos\s+pos,\s*BlockState\s+nextState,\s*boolean\s+movedByPiston\)\s*\{',
        $removalReplacement)
    $text = $text.Replace('super.onRemove(state, level, pos, nextState, movedByPiston);',
        'super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);')

    # BlockEntityRenderer gained the camera position and its off-screen hook
    # is now parameterless. Apply this only to renderer implementations;
    # utility render methods retain their canonical signatures.
    if ($relativePath -match '[\\/]client[\\/]render[\\/]' -and
        $text.Contains('implements BlockEntityRenderer')) {
        $text = $text.Replace('RenderType.entityCutoutNoCull', 'CompatRenderTypes.entityCutoutNoCull')
        $text = $text.Replace('RenderType.entityTranslucentEmissive', 'CompatRenderTypes.entityTranslucentEmissive')
        $text = $text.Replace('RenderType.lines', 'CompatRenderTypes.lines')
        $text = $text.Replace('RenderType.lightning', 'CompatRenderTypes.lightning')
        $text = $text.Replace('LightTexture.FULL_BRIGHT', '0x00F000F0')
        $text = $text.Replace('net.minecraft.client.renderer.ShapeRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('net.minecraft.client.renderer.LevelRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('ShapeRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('LevelRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = [regex]::Replace($text,
            '(int\s+(?:light|packedLight),\s*int\s+(?:overlay|packedOverlay))\)',
            '$1, net.minecraft.world.phys.Vec3 cameraPosition)')
        $text = [regex]::Replace($text,
            '(?m)shouldRenderOffScreen\([^\r\n)]*\)',
            'shouldRenderOffScreen()')
        $text = [regex]::Replace($text,
            'implements\s+BlockEntityRenderer<([^>]+)>',
            'extends LegacyBlockEntityRenderer<$1>')
        $text = [regex]::Replace($text,
            '(?m)(public\s+void)\s+render\(',
            '$1 legacyRender(', 1)
        if ($text.Contains('CompatRenderTypes.') -and
            -not $text.Contains('import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;')) {
            $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
                param($match) $match.Value + $newline +
                    'import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;'
            }, 1)
        }
        $text = $text.Replace('import net.minecraft.client.renderer.LightTexture;', '')
        if (-not $text.Contains('import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;')) {
            $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
                param($match) $match.Value + $newline +
                    'import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;'
            }, 1)
        }
    }

    if ($relativePath -match '[\\/]client[\\/]render[\\/]') {
        $text = $text.Replace('RenderType.entityCutoutNoCull', 'CompatRenderTypes.entityCutoutNoCull')
        $text = $text.Replace('RenderType.entityTranslucentEmissive', 'CompatRenderTypes.entityTranslucentEmissive')
        $text = $text.Replace('RenderType.lines', 'CompatRenderTypes.lines')
        $text = $text.Replace('RenderType.lightning', 'CompatRenderTypes.lightning')
        $text = $text.Replace('LightTexture.FULL_BRIGHT', '0x00F000F0')
        $text = $text.Replace('import net.minecraft.client.renderer.LightTexture;', '')
        $text = $text.Replace('net.minecraft.client.renderer.ShapeRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('net.minecraft.client.renderer.LevelRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('ShapeRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        $text = $text.Replace('LevelRenderer.renderLineBox',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox')
        if ($text.Contains('CompatRenderTypes.') -and
            -not $text.Contains('import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;')) {
            $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
                param($match) $match.Value + $newline +
                    'import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;'
            }, 1)
        }
    }

    # AbstractContainerScreen was converted to a deferred extractor in 26.1.
    # Keep the canonical screen implementation's stable render/input hooks in
    # a target adapter, while the adapter itself delegates to the official
    # MouseButtonEvent/KeyEvent/GuiGraphicsExtractor APIs.
    if ($relativePath -match '[\\/]client[\\/]screen[\\/].*Screen\.java$') {
        $text = $text.Replace('extends AbstractContainerScreen<',
            'extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<')
        $text = [regex]::Replace($text,
            '(?<!super)\.render\(graphics,\s*mouseX,\s*mouseY,\s*partialTick\)',
            '.extractRenderState(graphics, mouseX, mouseY, partialTick)')
        $text = [regex]::Replace($text,
            '(?<!super)\.keyPressed\(keyCode,\s*scanCode,\s*modifiers\)',
            '.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers))')
        $text = [regex]::Replace($text,
            '(?<!super)\.charTyped\(codePoint,\s*modifiers\)',
            '.charTyped(new net.minecraft.client.input.CharacterEvent(codePoint))')
        $text = [regex]::Replace($text,
            '(?<!super)\.mouseClicked\(mouseX,\s*mouseY,\s*button\)',
            '.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false)')
        $text = [regex]::Replace($text,
            '(?<!super)\.mouseDragged\(mouseX,\s*mouseY,\s*button,\s*dragX,\s*dragY\)',
            '.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), dragX, dragY)')
        $text = [regex]::Replace($text,
            '(?<!super)\.mouseReleased\(mouseX,\s*mouseY,\s*button\)',
            '.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)))')
        if (-not $text.Contains('import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;')) {
            $text = [regex]::Replace($text, '(?m)^package [^;]+;', {
                param($match) $match.Value + $newline +
                    'import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;'
            }, 1)
        }
    }

    if ($relativePath -match '[\\/]client[\\/]screen[\\/](TerminalTabButton|TerminalInventoryActionButton|FacePreviewButton)\.java$') {
        $text = $text.Replace('protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)',
            'protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)')
    }

    if ($relativePath -match '[\\/]client[\\/]keybind[\\/]ImmortalStorageKeybinds\.java$') {
        $text = $text.Replace(
            'public final class ImmortalStorageKeybinds {',
            'public final class ImmortalStorageKeybinds {' + $newline +
                '    private static final net.minecraft.client.KeyMapping.Category IMMORTALSTORAGE_CATEGORY =' + $newline +
                '            net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("immortalstorage", "immortalstorage"));')
        $text = $text.Replace('"key.categories.immortalstorage"',
            'IMMORTALSTORAGE_CATEGORY')
        $text = $text.Replace('OPEN_STORAGE.matches(event.getKeyCode(), event.getScanCode())',
            'OPEN_STORAGE.matches(new net.minecraft.client.input.KeyEvent(event.getKeyCode(), event.getScanCode(), 0))')
        $text = $text.Replace('long h = Minecraft.getInstance().getWindow()',
            'var h = Minecraft.getInstance().getWindow()')
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]ModBlockEntities\.java$') {
        $text = $text.Replace('ImmortalFurnaceBlockEntity::getItemHandler',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.getItemHandler(side))')
        $text = $text.Replace('SimulatedSpiritFieldBlockEntity::getItemHandler',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.getItemHandler(side))')
        $text = $text.Replace(
            ': new net.neoforged.neoforge.items.wrapper.RangedWrapper(',
            ': ' + $script:compatTransfer + '.item(new net.neoforged.neoforge.items.wrapper.RangedWrapper(')
        $text = $text.Replace(
            'SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT));',
            'SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT)));')
        $text = $text.Replace('(be, side) -> be.getItemHandler(side)',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.getItemHandler(side))')
        $text = $text.Replace('(be, side) -> be.getFluidHandler(side)',
            '(be, side) -> ' + $script:compatTransfer + '.fluid(be.getFluidHandler(side))')
        $text = $text.Replace('(be, side) -> be.getEnergyHandler(side)',
            '(be, side) -> ' + $script:compatTransfer + '.energy(be.getEnergyHandler(side))')
        $text = $text.Replace('(be, side) -> be.getItemHandler()',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.getItemHandler())')
        $text = $text.Replace('(be, side) -> be.getFluidHandler()',
            '(be, side) -> ' + $script:compatTransfer + '.fluid(be.getFluidHandler())')
        $text = $text.Replace('(be, side) -> be.itemHandler()',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.itemHandler())')
        $text = $text.Replace('(be, side) -> be.getCacheHandler()',
            '(be, side) -> ' + $script:compatTransfer + '.item(be.getCacheHandler())')
    }

    if ($relativePath -match '[\\/]client[\\/]screen[\\/]AbstractTerminalScreen\.java$') {
        # AbstractContainerScreen now stores the image dimensions as final
        # constructor state.  Pass the canonical terminal dimensions to the
        # official constructor before the legacy-compatible dynamic layout
        # fields are configured below.
        $text = [regex]::Replace($text,
            '(?m)(^\s*protected AbstractTerminalScreen\(M menu, Inventory inventory, Component title\) \{\r?\n)\s*super\(menu, inventory, title\);',
            '$1        super(menu, inventory, title, TerminalLayout.WIDTH, TerminalLayout.imageHeight(TerminalLayout.DEFAULT_ROWS, false));')
        $text = $text.Replace('this.minecraft.options.keyInventory.matches(keyCode, scanCode)',
            'this.minecraft.options.keyInventory.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers))')
        # GuiGraphicsExtractor queues GUI elements by stratum.  Amount labels
        # must be submitted after the item-model layer so the count remains a
        # readable foreground overlay on 26.1.2.
        $text = $text.Replace(
            'protected final void renderStorageAmountOverlays(GuiGraphicsExtractor graphics) {' + $newline,
            'protected final void renderStorageAmountOverlays(GuiGraphicsExtractor graphics) {' + $newline +
                '        graphics.nextStratum();' + $newline)
        # GuiGraphicsExtractor transforms scissor rectangles by the active
        # foreground matrix. Canonical 1.21.1 uses absolute coordinates, while
        # target foreground content is already translated by leftPos/topPos.
        # Rewrite only the dedicated content boundary; background clipping
        # remains screen-absolute because renderBg runs before that transform.
        $text = [regex]::Replace($text,
            '(?s)(protected final void enableStorageContentScissor\(GuiGraphicsExtractor graphics, Rect2i clip\) \{\s*)graphics\.enableScissor\(clip\.getX\(\), clip\.getY\(\),\s*clip\.getX\(\) \+ clip\.getWidth\(\), clip\.getY\(\) \+ clip\.getHeight\(\)\);',
            '$1graphics.enableScissor(clip.getX() - this.leftPos, clip.getY() - this.topPos,' + $newline +
                '                clip.getX() + clip.getWidth() - this.leftPos,' + $newline +
                '                clip.getY() + clip.getHeight() - this.topPos);')
    }

    if ($relativePath -match '[\\/]client[\\/]ClientSetup\.java$') {
        $text = $text.Replace(
            'import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;',
            'import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;' + $newline +
                'import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;')
        $text = $text.Replace(
            'modBus.addListener(ClientSetup::registerSpecialModelRenderers);',
            'modBus.addListener(ClientSetup::registerSpecialModelRenderers);' + $newline +
                '        modBus.addListener(ClientSetup::registerStaffModelProperties);')
        $staffRegistration =
            '    private static void registerStaffModelProperties(RegisterSelectItemModelPropertyEvent event) {' + $newline +
            '        event.register(Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "staff_mode"),' + $newline +
            '                SpiritStaffModeProperty.TYPE);' + $newline +
            '    }' + $newline + $newline
        $text = $text.Replace(
            '    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {',
            $staffRegistration + '    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {')
    }

    if ($relativePath -match '[\\/]client[\\/]screen[\\/]TerminalArmorSlot\.java$') {
        $text = [regex]::Replace($text,
            'public\s+Pair<Identifier,\s*Identifier>\s+getNoItemIcon\(\)\s*\{.*?\}',
            'public Identifier getNoItemIcon() { return noItemIcon != null ? noItemIcon.getFirst() : null; }')
    }

    if ($relativePath -match '[\\/]client[\\/]screen[\\/](XianqiaoInterfaceScreen|XianqiaoStorageScreen|AdvancedXianqiaoInterfaceScreen)\.java$') {
        # 26.1 exposes the real still sprite and stack tint through FluidModel.
        # Preserve the canonical fluid-slot presentation instead of substituting
        # a bucket item, which changes both identity and visual language.
        $text = [regex]::Replace($text,
            '(?s)    private void renderFluidSprite\(GuiGraphicsExtractor graphics, FluidStack stack, int x, int y\) \{.*?\r?\n    \}',
            '    private void renderFluidSprite(GuiGraphicsExtractor graphics, FluidStack stack, int x, int y) {' + $newline +
                '        if (stack == null || stack.isEmpty()) return;' + $newline +
                '        net.minecraft.client.renderer.block.FluidModel fluidModel = net.minecraft.client.Minecraft.getInstance().getModelManager()' + $newline +
                '                .getFluidStateModelSet().get(stack.getFluid().defaultFluidState());' + $newline +
                '        if (fluidModel == null || fluidModel.stillMaterial() == null) return;' + $newline +
                '        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();' + $newline +
                '        int tint = fluidModel.fluidTintSource() == null ? 0xFFFFFFFF' + $newline +
                '                : fluidModel.fluidTintSource().colorAsStack(stack);' + $newline +
                '        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(' + $newline +
                '                graphics, sprite, x, y, 16, 16, tint);' + $newline +
                '    }')
    }

    if ($relativePath -match '[\\/]compat[\\/]refinedstorage[\\/]RsCompat\.java$') {
        $text = [regex]::Replace($text,
            '(?s)RefinedStorageApi\.INSTANCE\.addGridResourceRepositoryMapper\(\s*RsExternalResource\.class,\s*RsExternalGridResourceMapper\.INSTANCE\);',
            'RefinedStorageApi.INSTANCE.getGridResourceTypeRegistry().register(EXTERNAL_RESOURCE_TYPE_ID, RsExternalGridResourceType.INSTANCE);')
    }
    if ($relativePath -match '[\\/]compat[\\/]refinedstorage[\\/]RsExternalResourceType\.java$') {
        $text = $text.Replace('import net.minecraft.network.chat.Component;' + $newline, '')
        $text = $text.Replace('import net.minecraft.network.chat.MutableComponent;' + $newline, '')
        $text = [regex]::Replace($text,
            '(?s)\s*@Override\s+public\s+MutableComponent\s+getTitle\(\)\s*\{.*?\}\s*@Override\s+public\s+Identifier\s+getSprite\(\)\s*\{.*?\}\s*',
            $newline)
        $text = $text.Replace('import net.minecraft.resources.Identifier;' + $newline, '')
    }
    if ($relativePath -match '[\\/]compat[\\/]refinedstorage[\\/]RsExternalGridResource\.java$') {
        $text = $text.Replace('import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;' + $newline, '')
        $text = $text.Replace('import com.refinedmods.refinedstorage.common.api.grid.view.GridResourceType;' + $newline, '')
        $text = $text.Replace('getAutocraftingRequest()', 'createAutocraftingRequest()')
        $text = $text.Replace('belongsToResourceType(ResourceType type)', 'is(GridResource other)')
        $text = $text.Replace('return type == RsExternalResourceType.INSTANCE;',
            'return other instanceof RsExternalGridResource that && resource.equals(that.resource);')
        $text = $text.Replace('    @Override public List<Component> getTooltip() {',
            '    @Override public com.refinedmods.refinedstorage.common.api.grid.view.GridResourceType getType() { return RsExternalGridResourceType.INSTANCE; }' + $newline + $newline +
                '    @Override public List<Component> getTooltip() {')
        $text = $text.Replace('    @Override public int getRegistryId() {',
            '    RsExternalResource externalResource() { return resource; }' + $newline + $newline +
                '    @Override public int getRegistryId() {')
    }
    if ($relativePath -match '[\\/]compat[\\/]refinedstorage[\\/]RsExternalResourceRendering\.java$') {
        $text = $text.Replace('import net.minecraft.client.renderer.MultiBufferSource;' + $newline, '')
        $text = $text.Replace('import net.minecraft.world.level.Level;' + $newline, '')
        $text = $text.Replace('import net.minecraft.client.renderer.MultiBufferSource;', '')
        $text = $text.Replace('import net.minecraft.world.level.Level;', '')
        $text = [regex]::Replace($text,
            'ResourceKey resource,\s*PoseStack poseStack,\s*MultiBufferSource buffers,\s*int light,\s*Level level',
            'ResourceKey resource, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector,' + $newline +
                '            int light, long seed)')
        $text = $text.Replace('long seed))', 'long seed)')
    }
    if ($relativePath -match 'XianqiaoRsStorage\.java$') {
        $text = $text.Replace('import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;' + $newline,
            'import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;' + $newline +
                'import com.refinedmods.refinedstorage.common.api.storage.StorageContents;' + $newline)
        $text = $text.Replace('import java.util.Objects;' + $newline,
            'import java.util.Objects;' + $newline + 'import java.util.Optional;' + $newline)
        $text = [regex]::Replace($text, '(?m)^    @Override\s+public StorageType getType\(\) \{',
            '    @Override' + $newline +
                '    public com.refinedmods.refinedstorage.common.api.storage.StorageContents toContents() {' + $newline +
                '        List<com.refinedmods.refinedstorage.common.api.storage.StorageContents.Stored> stored = getAll().stream()' + $newline +
                '                .map(entry -> new com.refinedmods.refinedstorage.common.api.storage.StorageContents.Stored(entry.resource(), entry.amount(), java.util.Optional.empty()))' + $newline +
                '                .toList();' + $newline +
                '        return new com.refinedmods.refinedstorage.common.api.storage.StorageContents(getType(), java.util.Optional.empty(), stored);' + $newline +
                '    }' + $newline + $newline +
                '    @Override' + $newline + '    public StorageType getType() {')
    }

    # AE2 26.1 moved its client key renderer API to appeng.client.api and
    # replaced the old immediate world-face callback with a render-state pair.
    # The external channel has no world-block representation, so the official
    # state hooks are intentionally no-ops while GUI drawing and tooltips stay
    # fully implemented.
    if ($relativePath -match '[\\/]compat[\\/]ae2[\\/]Ae2ClientCompat\.java$') {
        $text = $text.Replace('import appeng.api.client.AEKeyRendering;',
            'import appeng.client.api.AEKeyRendering;')
    }
    if ($relativePath -match '[\\/]compat[\\/]ae2[\\/]ImmortalStorageExternalResourceKeyRenderHandler\.java$') {
        $text = $text.Replace('import appeng.api.client.AEKeyRenderHandler;',
            'import appeng.client.api.AEKeyRenderer;')
        $text = $text.Replace('import net.minecraft.client.renderer.MultiBufferSource;' + $newline, '')
        $text = $text.Replace('implements AEKeyRenderHandler<ImmortalStorageExternalResourceKey>',
            'implements AEKeyRenderer<ImmortalStorageExternalResourceKey, Void>')
        $text = [regex]::Replace($text,
            '(?s)    @Override\s+public void drawOnBlockFace\(PoseStack poseStack, MultiBufferSource buffers,\s*ImmortalStorageExternalResourceKey key, float partialTick,\s*int light, Level level\)\s*\{.*?\n    \}',
            '    @Override' + $newline +
                '    public Class<Void> stateClass() { return Void.class; }' + $newline + $newline +
                '    @Override' + $newline +
                '    public Void createState() { return null; }' + $newline + $newline +
                '    @Override' + $newline +
                '    public void extract(Void state, ImmortalStorageExternalResourceKey key, Level level, int seed) {}' + $newline + $newline +
                '    @Override' + $newline +
                '    public void submit(PoseStack poseStack, Void state, net.minecraft.client.renderer.SubmitNodeCollector collector, int light) {}')
        $text = $text.Replace('drawInGui(Minecraft minecraft, GuiGraphics graphics,',
            'drawInGui(Minecraft minecraft, GuiGraphicsExtractor graphics,')
        # AEKeyRenderer supplies tooltip components directly; the old
        # getDisplayName callback belonged to AEKeyRenderHandler and is not
        # part of the official 26.1 API.
        $text = [regex]::Replace($text,
            '(?s)    @Override\s+public Component getDisplayName\(ImmortalStorageExternalResourceKey key\)\s*\{.*?\n    \}\s*',
            '')
        $text = $text.Replace('List.of(getDisplayName(key))',
            'List.of(ExternalResourceCatalog.displayName(key.resource()))')
    }
    if ($relativePath -match '[\\/]compat[\\/]ae2[\\/]ImmortalStorageExternalResourceKey\.java$') {
        $text = $text.Replace('import net.minecraft.core.HolderLookup;', '')
        $text = $text.Replace('import net.minecraft.nbt.CompoundTag;', '')
        $text = $text.Replace('import net.minecraft.world.level.Level;',
            'import net.minecraft.world.level.Level;' + $newline +
                'import net.minecraft.world.level.storage.ValueInput;' + $newline +
                'import net.minecraft.world.level.storage.ValueOutput;' + $newline)
        $text = [regex]::Replace($text,
            '(?s)    @Override\s+public CompoundTag toTag\(HolderLookup\.Provider registries\)\s*\{.*?\n    \}',
            '    @Override' + $newline +
                '    public void toTag(ValueOutput output) {' + $newline +
                '        output.putString("channel", resource.channel());' + $newline +
                '        output.putString("resource", resource.resourceId());' + $newline +
                '    }')
        $text = [regex]::Replace($text,
            '(?s)    static @Nullable ImmortalStorageExternalResourceKey fromTag\(CompoundTag tag\)\s*\{.*?\n    \}',
            '    static @Nullable ImmortalStorageExternalResourceKey fromTag(ValueInput input) {' + $newline +
                '        String channel = input.getString("channel").orElse(null);' + $newline +
                '        String resourceId = input.getString("resource").orElse(null);' + $newline +
                '        if (channel == null || resourceId == null) return null;' + $newline +
                '        try {' + $newline +
                '            return new ImmortalStorageExternalResourceKey(channel, resourceId);' + $newline +
                '        } catch (IllegalArgumentException exception) {' + $newline +
                '            return null;' + $newline +
                '        }' + $newline +
                '    }')
    }
    if ($relativePath -match '[\\/]compat[\\/]ae2[\\/]ImmortalStorageExternalResourceKeyType\.java$') {
        $text = $text.Replace('import net.minecraft.core.HolderLookup;', '')
        $text = $text.Replace('import net.minecraft.nbt.CompoundTag;', '')
        $text = $text.Replace('import net.minecraft.network.RegistryFriendlyByteBuf;',
            'import net.minecraft.network.RegistryFriendlyByteBuf;' + $newline +
                'import net.minecraft.world.level.storage.ValueInput;' + $newline)
        $text = $text.Replace('loadKeyFromTag(HolderLookup.Provider registries, CompoundTag tag)',
            'loadKeyFromTag(ValueInput tag)')
    }
    if ($relativePath -match '[\\/]client[\\/]render[\\/]OneQiBeamRenderer\.java$') {
        $text = $text.Replace('public static void render(RenderLevelStageEvent event) {',
            'public static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {')
        $text = $text.Replace('event.getCamera().getPosition()',
            'event.getLevelRenderState().cameraRenderState.pos')
        $text = $text.Replace('event.getCamera().position()',
            'event.getLevelRenderState().cameraRenderState.pos')
        $text = $text.Replace('event.getPartialTick().getGameTimeDeltaPartialTick(false)',
            'minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)')
        $text = [regex]::Replace($text,
            '(?m)^\s*if \(event\.getStage\(\) != RenderLevelStageEvent\.Stage\.AFTER_PARTICLES\) return;\s*\r?\n',
            '')
    }

    if ($relativePath -match '[\\/]client[\\/]render[\\/]OneQiHeldItemMuzzle\.java$') {
        $text = $text.Replace('getMainCamera().getPosition()', 'getMainCamera().position()')
    }

    if ($relativePath -match '[\\/]block[\\/]entity[\\/]YuanLightIndex\.java$') {
        $text = $text.Replace('center.x', 'center.x()').Replace('center.z', 'center.z()')
    }

    if ($relativePath -match '[\\/]item[\\/]custom[\\/](SpiritSwordItem|SpiritStaffItem)\.java$') {
        $text = [regex]::Replace($text,
            '(?m)^\s*@Override\s*\r?\n\s*(public int getEnchantmentValue)',
            '    $1')
    }
    if ($relativePath -match '[\\/]item[\\/]custom[\\/](ImmortalYuanItem|SpiritDriveItem|TrueYuanItem)\.java$') {
        $text = $text.Replace('@Override public int getBurnTime', 'public int getBurnTime')
        $text = [regex]::Replace($text,
            '(?m)^\s*@Override\s*\r?\n\s*(public int getBurnTime)',
            '    $1')
    }

    if ($relativePath -match '[\\/]client[\\/]render[\\/]SpiritStaffBuildPreview\.java$') {
        $text = $text.Replace('import net.neoforged.neoforge.client.event.RenderHighlightEvent;',
            'import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;')
        $previewMethod = @'
    private static void renderWorldPreview(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            reset();
            return;
        }
        InteractionHand hand = buildHand(minecraft.player.getMainHandItem(),
                minecraft.player.getOffhandItem());
        if (hand == null) {
            reset();
            return;
        }

        BlockHitResult hit = event.getHitResult();
        boolean removal = ImmortalStorageKeybinds.SPECIAL_OPERATION.isDown();
        PreviewTarget current = new PreviewTarget(
                hit.getBlockPos().asLong(), hit.getDirection().ordinal(), hand.ordinal(), removal);
        long gameTick = minecraft.level.getGameTime();
        boolean targetChanged = !current.equals(requestedTarget);
        if (targetChanged || gameTick - lastRequestTick >= 5L) {
            requestedTarget = current;
            lastRequestTick = gameTick;
            requestId = requestId == Integer.MAX_VALUE ? 1 : requestId + 1;
            if (targetChanged) {
                serverPositions = List.of();
                previewFailure = null;
            }
            ClientPacketDistributor.sendToServer(new ModPayloads.RequestSpiritStaffBuildPreview(
                    requestId, hit.getBlockPos(), hit.getDirection().ordinal(), hand.ordinal(), removal));
        }
        if (previewFailure == null || serverPositions.isEmpty()) return;

        event.addCustomRenderer((outlineState, buffers, poseStack, highContrast, levelRenderState) -> {
            Vec3 camera = event.getCamera().position();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            VertexConsumer lines = buffers.getBuffer(CompatRenderTypes.lines());
            for (BlockPos pos : serverPositions) {
                float red = requestedTarget.removal() ? 1.0F : 0.15F;
                float green = requestedTarget.removal() ? 0.15F : 0.95F;
                float blue = requestedTarget.removal() ? 0.15F : 0.85F;
                com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox(
                        poseStack, lines, new AABB(pos).inflate(0.002D), red, green, blue, 0.9F);
            }
            poseStack.popPose();
            return true;
        });
    }

    private static void renderHudCount
'@
        $text = [regex]::Replace($text,
            '(?s)    private static void renderWorldPreview\(RenderHighlightEvent\.Block event\) \{.*?\r?\n    \}\r?\n\r?\n    private static void renderHudCount',
            $previewMethod)
    }

    if ($relativePath -match '[\\/]client[\\/]render[\\/]SimulatedReincarnationFurnaceRenderer\.java$') {
        # EntityRenderDispatcher now extracts the render state itself; the
        # former yaw-plus-partial-tick pair collapses to one partial-tick arg.
        $text = $text.Replace('0.0F, partialTick, poses, buffers, 0x00F000F0',
            'partialTick, poses, buffers, 0x00F000F0')
    }

    # The 26.1.2 GUI is a deferred render-state builder. Replace legacy
    # immediate flush/depth calls and the old three-dimensional GUI matrix
    # operations with the official Matrix3x2fStack operations.
    if ($relativePath -match '[\\/]client[\\/]screen[\\/]AbstractTerminalScreen\.java$') {
        # The target extractor has no immediate flush operation.  Advancing to
        # the next deferred stratum is the official foreground-pass boundary.
        $text = $text.Replace('graphics.flush();', 'graphics.nextStratum();')
    } else {
        $text = $text.Replace('graphics.flush();', '')
    }
    $text = $text.Replace('getTimer().getGameTimeDeltaPartialTick',
        'getDeltaTracker().getGameTimeDeltaPartialTick')
    if ($relativePath -match '[\\/]client[\\/]' -or $text.Contains('GuiGraphics')) {
        # GuiGraphics owns the deferred depth state in 26.1.2. These direct
        # RenderSystem toggles are both unavailable and redundant there.
        $text = $text.Replace('RenderSystem.disableDepthTest();', '')
        $text = $text.Replace('RenderSystem.enableDepthTest();', '')
        $text = $text.Replace('graphics.pose().pushPose()', 'graphics.pose().pushMatrix()')
        $text = $text.Replace('graphics.pose().popPose()', 'graphics.pose().popMatrix()')
        $text = [regex]::Replace($text,
            'graphics\.pose\(\)\.translate\(([^,\r\n]+),\s*([^,\r\n]+),\s*[^)\r\n]+\)',
            'graphics.pose().translate($1, $2)')
        $text = [regex]::Replace($text,
            'graphics\.pose\(\)\.scale\(([^,\r\n]+),\s*([^,\r\n]+),\s*[^)\r\n]+\)',
            'graphics.pose().scale($1, $2)')
        # The terminal slot helpers contain their own argument comma, which
        # cannot be split safely by the compact legacy-call regex above.
        $text = [regex]::Replace($text,
            'graphics\.pose\(\)\.translate\(visualSlotX\(menuIndex, slot\) - slot\.x\)\s*-\s*slot\.y,\s*0\.0F\)',
            'graphics.pose().translate(visualSlotX(menuIndex, slot) - slot.x, visualSlotY(menuIndex, slot) - slot.y)')
        $text = [regex]::Replace($text,
            'graphics\.pose\(\)\.translate\(x \+ 17\.0F,\s*y \+ storageAmountBottomOffset\(relative, amount\),\s*TerminalLayout\.STORAGE_AMOUNT_Z\)',
            'graphics.pose().translate(x + 17.0F, y + storageAmountBottomOffset(relative, amount))')
        $text = $text.Replace('storageAmountBottomOffset(relative)', 'storageAmountBottomOffset(relative, amount)')
        if ($relativePath -match '[\\/]client[\\/]screen[\\/]AbstractTerminalScreen\.java$') {
            # 26.1.2 made AbstractContainerScreen's highlight hook private.
            # Keep the canonical helper available to the screen without
            # pretending to override a method that no longer exists.
            $text = [regex]::Replace($text,
                '(?m)^\s*@Override\s*\r?\n\s*protected void renderSlotHighlight\(',
                '    private void renderSlotHighlightCompat(')
            $text = $text.Replace('super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);', '')
        }
        if ($relativePath -match '[\\/]client[\\/]screen[\\/]' -or $text.Contains('GuiGraphics')) {
            $text = [regex]::Replace($text,
                '(?s)graphics\.renderTooltip\(\s*this\.font,\s*(.*?),\s*(?:java\.util\.)?Optional\.empty\(\),\s*mouseX,\s*mouseY\);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, this.font, $1, mouseX, mouseY);')
            $text = $text.Replace('graphics.renderTooltip(this.font, this.title, mouseX, mouseY);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, this.font, this.title, mouseX, mouseY);')

            $text = $text.Replace('graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F, 256, 256);')
            $text = [regex]::Replace($text,
                'graphics\.blit\(definition\.icon\(\),\s*x,\s*y,\s*0\.0F,\s*0\.0F,\s*16,\s*16,\s*16,\s*externalTextureHeight\(key\)\);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, definition.icon(), x, y, 16, 16, 0.0F, 0.0F, 16, externalTextureHeight(key));')
            $text = [regex]::Replace($text,
                'graphics\.blit\(x,\s*y,\s*0,\s*TerminalLayout\.SLOT_SIZE,\s*TerminalLayout\.SLOT_SIZE,\s*sprite\);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, sprite, x, y, TerminalLayout.SLOT_SIZE, TerminalLayout.SLOT_SIZE, tint);')
            $text = [regex]::Replace($text,
                'graphics\.blit\(x,\s*y,\s*0,\s*16,\s*16,\s*sprite\);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, sprite, x, y, 16, 16, tint);')
            $text = [regex]::Replace($text,
                'graphics\.blit\(([^,\r\n]+),\s*([^,\r\n]+),\s*0,\s*([^,\r\n]+),\s*([^,\r\n]+),\s*(\w+)\);',
                'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, $6, $1, $2, $3, $4);')
            $text = $text.Replace('graphics.blitSprite(', 'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, ')
            $text = $text.Replace('graphics.blit(', 'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, ')
            $text = $text.Replace('g.blitSprite(', 'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(g, ')
            $text = $text.Replace('g.blit(', 'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(g, ')
            $text = [regex]::Replace($text,
                '(?s)\s*graphics\.setColor\(\(\(tint >>> 16\).*?\);',
                '')
            $text = [regex]::Replace($text,
                '(?m)^\s*graphics\.setColor\(1\.0F,\s*1\.0F,\s*1\.0F,\s*1\.0F\);\s*\r?\n?', '')
            $text = [regex]::Replace($text,
                '(?m)^\s*graphics\.setColor\(red,\s*green,\s*blue,\s*alpha\);\s*\r?\n?', '')
            foreach ($legacyColorLine in @(
                '        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;' + $newline,
                '        float red = ((tint >>> 16) & 0xFF) / 255.0F;' + $newline,
                '        float green = ((tint >>> 8) & 0xFF) / 255.0F;' + $newline,
                '        float blue = (tint & 0xFF) / 255.0F;' + $newline,
                '        graphics.setColor(red, green, blue, alpha);' + $newline,
                '        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);' + $newline)) {
                $text = $text.Replace($legacyColorLine, '')
            }
        }
    }
    if ($relativePath -match '[\\/]client[\\/]render[\\/].*(Decorator|GuiItemPreviewOverlay)\.java$') {
        $text = $text.Replace('pose.pushPose()', 'pose.pushMatrix()')
        $text = $text.Replace('pose.popPose()', 'pose.popMatrix()')
        $text = [regex]::Replace($text,
            'pose\.translate\(([^,\r\n]+),\s*([^,\r\n]+),\s*[^)\r\n]+\)',
            'pose.translate($1, $2)')
        $text = [regex]::Replace($text,
            'pose\.scale\(([^,\r\n]+),\s*([^,\r\n]+),\s*[^)\r\n]+\)',
            'pose.scale($1, $2)')
    }
    if ($relativePath -match '[\\/]worldshard[\\/]WorldShardOreScanner\.java$') {
        $text = $text.Replace('Set<ConfiguredFeature<?, ?>> visitedConfigured =',
            'Set<net.minecraft.core.Holder<ConfiguredFeature<?, ?>>> visitedConfigured =')
        $text = $text.Replace('describeOreConfiguration(configured.config())',
            'describeOreConfiguration(configured.value().config())')
    }

    # 26.1.2 retains JSON resources but the listener constructor now consumes
    # ExtraCodecs.JSON instead of Gson.
    if ($relativePath -match '(?:SourceDefinitionReloadListener|WorldShardMinerReloadListener|WorldShardLootReloadListener|SimulatedSpiritFieldCropCatalog)\.java$') {
        $text = $text.Replace('extends SimpleJsonResourceReloadListener',
            'extends SimpleJsonResourceReloadListener<JsonElement>')
        $text = $text.Replace('super(GSON, DIRECTORY);',
            'super(net.minecraft.util.ExtraCodecs.JSON, net.minecraft.resources.FileToIdConverter.json(DIRECTORY));')
    }

    # The official 26.1 furnace keeps the same feature entry points but its
    # private helper contracts changed: burn now receives three stacks and
    # getBurnDuration receives FuelValues.  Keep the spirit-drive/sword
    # behaviour active in the target mixin instead of shipping a stale 1.21.1
    # shadow or redirect that fails during Mixin bootstrap.
    if ($relativePath -match '[\\/]mixin[\\/]core[\\/]AbstractFurnaceSpiritDriveMixin\.java$') {
        $text = $text.Replace(
            '@Shadow protected abstract int getBurnDuration(ItemStack stack);',
            '@Shadow protected abstract int getBurnDuration(net.minecraft.world.level.block.entity.FuelValues fuelValues, ItemStack stack);')
        $text = $text.Replace(
            'getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I',
            'getBurnDuration(Lnet/minecraft/world/level/block/entity/FuelValues;Lnet/minecraft/world/item/ItemStack;)I')
        $text = $text.Replace(
            'AbstractFurnaceBlockEntity furnace, ItemStack fuel,',
            'AbstractFurnaceBlockEntity furnace, net.minecraft.world.level.block.entity.FuelValues fuelValues, ItemStack fuel,')
        $text = $text.Replace('getBurnDuration(fuel);', 'getBurnDuration(fuelValues, fuel);')
        $text = $text.Replace(
            '            Level level, net.minecraft.core.BlockPos pos,',
            '            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,')
        $text = [regex]::Replace($text,
            '(@Redirect\(method = ")serverTick(",\s+at = @At\(value = "INVOKE",\s+target = "Lnet/minecraft/world/item/ItemStack;shrink\(I\)V"\)\))',
            '$1consumeFuel$2')
        $driveReplacement = '$1net.minecraft.core.NonNullList<ItemStack> items,' + $newline +
            '            ItemStack fuelArgument)'
        $text = [regex]::Replace($text,
            '(?s)(private static void immortalstorage\$keepSpiritDrive\(\s*ItemStack fuel, int amount,\s*)net\.minecraft\.server\.level\.ServerLevel level, net\.minecraft\.core\.BlockPos pos,\s*net\.minecraft\.world\.level\.block\.state\.BlockState state,\s*AbstractFurnaceBlockEntity furnace\)',
            $driveReplacement)
    }
    if ($relativePath -match '[\\/]mixin[\\/]core[\\/]AbstractFurnaceSpiritSwordTemperingMixin\.java$') {
        $text = $text.Replace('import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;',
            'import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;')
        $text = $text.Replace('net.minecraft.core.RegistryAccess registries,', '')
        $text = $text.Replace('net.minecraft.world.item.crafting.RecipeHolder<?> recipe,', '')
        $text = $text.Replace('NonNullList<ItemStack> items, int maxStack,',
            'NonNullList<ItemStack> items, ItemStack fuel, ItemStack result,')
        $text = $text.Replace('AbstractFurnaceBlockEntity furnace,', '')
        $text = $text.Replace('CallbackInfoReturnable<Boolean> cir', 'CallbackInfo cir')
        $text = $text.Replace(' || recipe == null', '')
        $text = $text.Replace('cir.setReturnValue(true);', 'cir.cancel();')
    }

    # Final residual cleanup for signatures whose old type appears only in a
    # simple-name use site (imports were already rewritten above).
    $text = $text.Replace('InteractionResultHolder<ItemStack>', 'InteractionResult')
    $text = $text.Replace('InteractionResultHolder.consume(player.getItemInHand(hand))',
        'InteractionResult.CONSUME')
    $text = $text.Replace('UseAnim.BOW', 'ItemUseAnimation.BOW')
    $text = $text.Replace('public UseAnim getUseAnimation', 'public ItemUseAnimation getUseAnimation')
    $text = [regex]::Replace($text,
        'graphics\.pose\(\)\.translate\(x \+ 17\.0F,\s*y \+ storageAmountBottomOffset\(relative, amount\),\s*TerminalLayout\.STORAGE_AMOUNT_Z\)',
        'graphics.pose().translate(x + 17.0F, y + storageAmountBottomOffset(relative, amount))')

    # Tests are migrated with the same source transform, but a few assertions
    # construct vanilla objects directly.  Keep these adaptations confined to
    # the generated test lane so the canonical 1.21.1 tests remain an honest
    # executable specification for their own API.
    if ($relativePath -match '(^|[\\/])src[\\/]test[\\/]') {
        # Source-contract tests are executed from the canonical MDK root.  In
        # the migration lane their inspected Java tree is the generated 26.1.2
        # overlay, while resources/build metadata remain owned by the MDK.
        $text = $text.Replace(
            '"src", "main", "java"',
            '"..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source"')
        $text = $text.Replace(
            '"src/main/java"',
            '"../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"')
        # The shared core is not part of the loader-specific audit tree.
        $text = $text.Replace(
            '"..", "immortalstorage-core", "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source"',
            '"..", "immortalstorage-core", "src", "main", "java"')
        # A few legacy contract tests build the canonical source path as one
        # string literal rather than from the separate "src/main/java"
        # segments handled above.  Point those readers at the merged target
        # audit tree as well; otherwise they silently inspect 1.21.1 code.
        $targetAuditJavaRoot = '../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage'
        $text = $text.Replace(
            '"src/main/java/com/immortalstorage/immortalstorage"',
            '"' + $targetAuditJavaRoot + '"')

        if ($relativePath -match '[\\/]menu[\\/]custom[\\/]XianqiaoRealmTimeLayoutContractTest\.java$') {
            $text = $text.Replace(
                'Path root = locateMainSourceRoot();',
                'Path root = locateMainSourceRoot().getParent().getParent().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source")).normalize();')
            $text = $text.Replace(
                '"java/com/immortalstorage/immortalstorage/client/screen/XianqiaoStorageScreen.java"',
                '"com/immortalstorage/immortalstorage/client/screen/XianqiaoStorageScreen.java"')
        }
        if ($relativePath -match '[\\/]compat[\\/]CreateSchematicannonStorageContractTest\.java$') {
            $text = $text.Replace(
                'locateMainSourceRoot().resolve("java/com/immortalstorage/immortalstorage/block/entity")',
                'locateMainSourceRoot().getParent().getParent().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "block", "entity")).normalize()')
        }
        if ($relativePath -match '[\\/]client[\\/]render[\\/]DynamicItemPreviewContractTest\.java$') {
            $oldReadResource = @'
    private static String readResource(String relative) throws IOException {
        Path mainJava = MAIN;
        Path main = mainJava;
        for (int i = 0; i < 4; i++) main = main.getParent();
        return Files.readString(main.resolve("resources").resolve(relative));
    }
'@
            $newReadResource = @'
    private static String readResource(String relative) throws IOException {
        Path resources = MAIN;
        for (int i = 0; i < 6; i++) resources = resources.getParent();
        resources = resources.resolve(Path.of("src", "main", "resources"));
        return Files.readString(resources.resolve(relative));
    }
'@
            $text = $text.Replace($oldReadResource, $newReadResource)
        }
        if ($relativePath -match '[\\/]compat[\\/]PatchouliBundledHandbookContractTest\.java$') {
            $text = $text.Replace(
                'private static final Path ROOT = PROJECT_ROOT.resolve("src/main");',
                'private static final Path ROOT = PROJECT_ROOT.resolve("src/main");' + $newline +
                    '    private static final Path TARGET_SOURCE_ROOT = Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source");')
            $text = $text.Replace(
                'ROOT.resolve("java/com/immortalstorage/immortalstorage/item/SimpleJadeGuideItem.java")',
                'TARGET_SOURCE_ROOT.resolve("com/immortalstorage/immortalstorage/item/SimpleJadeGuideItem.java")')
            $text = $text.Replace(
                'ROOT.resolve("java/com/immortalstorage/immortalstorage/network/ModPayloads.java")',
                'TARGET_SOURCE_ROOT.resolve("com/immortalstorage/immortalstorage/network/ModPayloads.java")')
            $text = $text.Replace(
                "jarJar('vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE')",
                "jarJar('vazkii.patchouli:patchouli-neoforge:26.1-94')")
        }
        if ($relativePath -match '[\\/]item[\\/]SpiritStaffVisualResourceTest\.java$') {
            $text = $text.Replace(
                'private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));',
                'private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));' + $newline +
                    '    private static final Path TARGET_RESOURCES = PROJECT.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "main", "resources"));')
            $text = $text.Replace(
                'String model = Files.readString(RESOURCES.resolve(Path.of(',
                'String model = Files.readString(TARGET_RESOURCES.resolve(Path.of(')
            $text = $text.Replace(
                'TARGET_RESOURCES.resolve(Path.of("assets", "immortalstorage", "models", "item", "spirit_staff.json"))',
                'TARGET_RESOURCES.resolve(Path.of("assets", "immortalstorage", "items", "spirit_staff.json"))')
            $text = [regex]::Replace($text,
                'TARGET_RESOURCES\.resolve\(Path\.of\(\s*"assets", "immortalstorage", "models", "item", "spirit_staff\.json"\s*\)\)',
                'TARGET_RESOURCES.resolve(Path.of("assets", "immortalstorage", "items", "spirit_staff.json"))')
            $text = $text.Replace(
                'assertTrue(model.contains("\"immortalstorage:staff_mode\": 1.0"));',
                'assertTrue(model.contains("\"type\": \"minecraft:select\""));')
            $text = $text.Replace(
                'assertTrue(model.contains("\"immortalstorage:staff_mode\": 2.0"));',
                'assertTrue(model.contains("\"property\": \"immortalstorage:staff_mode\""));')
            $text = $text.Replace(
                'assertTrue(model.contains("\"immortalstorage:staff_mode\": 3.0"));',
                'assertTrue(model.contains("\"when\": \"teleport\""));')
            $text = $text.Replace(
                'assertTrue(clientSetup.contains("ItemProperties.register("));',
                'assertTrue(clientSetup.contains("RegisterSelectItemModelPropertyEvent"));')
            $text = $text.Replace(
                'assertTrue(clientSetup.contains("SpiritStaffItem.getMode(stack)"));',
                'assertTrue(clientSetup.contains("SpiritStaffModeProperty.TYPE"));')
        }
        # NeoForge 26.1 leaves vanilla default data components unbound until
        # the server resource reload phase.  Unit tests do not run that phase,
        # so every migrated class receives a small same-class @BeforeAll hook.
        # Keeping the hook in the test class avoids the transformed-classloader
        # boundary that breaks JUnit service-loader extensions.
        if (-not $text.Contains('immortalStorageTargetBootstrap()')) {
            $bootstrapMethod = $newline +
                '    @org.junit.jupiter.api.BeforeAll' + $newline +
                '    static void immortalStorageTargetBootstrap() {' + $newline +
                '        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();' + $newline +
                '    }' + $newline
            $text = [regex]::Replace($text,
                '(?m)^((?:public\s+)?(?:final\s+)?class\s+\w+[^\r\n]*\{)',
                '$1' + $bootstrapMethod, 1)
        }
        # Item.Properties now requires an item id before an ItemStack can be
        # created.  These migrated tests register their custom items directly,
        # so the property id must match the Registry.register key exactly.
        # Limit the replacement to those files so source-inspection assertions
        # remain unchanged.
        if ($relativePath -match 'WorldShardUnifiedOutputTest\.java$') {
            $text = $text.Replace(
                'new Item.Properties().stacksTo(64)',
                'new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_world_shard_test", "immortal_yuan"))).stacksTo(64)')
        }
        if ($relativePath -match 'ImmortalStoragePlayerDeferredYuanTest\.java$') {
            $text = $text.Replace(
                'new TrueYuanItem(new Item.Properties().stacksTo(64))',
                'new TrueYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_deferred_test", "true_yuan"))).stacksTo(64))')
            $text = $text.Replace(
                'new ImmortalYuanItem(new Item.Properties().stacksTo(64))',
                'new ImmortalYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_deferred_test", "immortal_yuan"))).stacksTo(64))')
        }
        if ($relativePath -match 'TribulationStateTest\.java$') {
            $text = $text.Replace(
                'new ImmortalYuanItem(new Item.Properties().stacksTo(64))',
                'new ImmortalYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_tribulation_test", "immortal_yuan"))).stacksTo(64))')
        }
        if ($relativePath -match 'YuanStorageBoundaryTest\.java$') {
            $text = $text.Replace(
                'new TrueYuanItem(new Item.Properties().stacksTo(64))',
                'new TrueYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_test", "true_yuan"))).stacksTo(64))')
            $text = $text.Replace(
                'new ImmortalYuanItem(new Item.Properties().stacksTo(64))',
                'new ImmortalYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(' +
                    'net.minecraft.core.registries.Registries.ITEM, ' +
                    'net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_test", "immortal_yuan"))).stacksTo(64))')
        }
        if ($relativePath -match '(WorldShardUnifiedOutputTest|ImmortalStoragePlayerDeferredYuanTest|TribulationStateTest|YuanStorageBoundaryTest)\.java$') {
            $text = $text.Replace(
                '        BuiltInRegistries.ITEM.freeze();',
                '        BuiltInRegistries.ITEM.freeze();' + $newline +
                    '        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.rebindItemComponents();')
        }
        $text = $text.Replace('import appeng.api.client.AEKeyRendering;',
            'import appeng.client.api.AEKeyRendering;')
        $text = $text.Replace('import net.minecraft.world.item.component.Unbreakable;', '')
        $text = $text.Replace('new Unbreakable(false)', 'net.minecraft.util.Unit.INSTANCE')
        $text = $text.Replace('new Inventory(null)',
            'new Inventory(null, new net.minecraft.world.entity.EntityEquipment())')
        $text = $text.Replace(
            'sword.getAttributeModifiers().compute(1.0D, EquipmentSlot.MAINHAND)',
            'sword.getAttributeModifiers().compute(' +
                'net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ' +
                '1.0D, EquipmentSlot.MAINHAND)')
        $text = $text.Replace('.unfreeze();', '.unfreeze(false);')
        $text = $text.Replace('immortalYuan.hasCraftingRemainingItem()',
            'immortalYuan.getItem().getCraftingRemainder() != null && immortalYuan.getItem().getCraftingRemainder().count() > 0')
        $text = [regex]::Replace($text,
            'BuiltInRegistries\.POINT_OF_INTEREST_TYPE\s*\.\s*getHolderOrThrow\(([^)]+)\)',
            'BuiltInRegistries.POINT_OF_INTEREST_TYPE.get($1).orElseThrow()')

        # UUID tags are represented by the official int-array codec in 26.1.
        # Reuse the target compatibility helper so all migrated tests exercise
        # the same persistence representation as production code.
        $text = [regex]::Replace($text,
            '(?m)(?<receiver>[A-Za-z_][A-Za-z0-9_]*)\.putUUID\((?<args>[^;\r\n]+)\);',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(${receiver}, ${args});')
        $text = [regex]::Replace($text,
            '(?<receiver>[A-Za-z_][A-Za-z0-9_]*)\.getUUID\((?<key>[^)]+)\)',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(${receiver}, ${key})')

        # The target block-item bridge intentionally stores the machine
        # payload in CUSTOM_DATA.  This keeps the test aligned with the
        # target CompatBlockEntity implementation instead of calling the
        # removed CompoundTag overload of BlockItem.setBlockEntityData.
        $text = $text.Replace(
            'BlockItem.setBlockEntityData(boundBlock, BlockEntityType.FURNACE, ownerData);',
            'boundBlock.set(DataComponents.CUSTOM_DATA, ' +
                'net.minecraft.world.item.component.CustomData.of(ownerData));')

        # AE2 26.1 serializes keys through ValueOutput/ValueInput rather than
        # the old nullable CompoundTag overloads.
        $text = $text.Replace('key.toTag(null)',
            'com.immortalstorage.immortalstorage.compat.CompatTestNbt.toTag(key)')
        $text = $text.Replace(
            'ImmortalStorageExternalResourceKey.fromTag(new net.minecraft.nbt.CompoundTag())',
            'ImmortalStorageExternalResourceKey.fromTag(' +
                'com.immortalstorage.immortalstorage.compat.CompatTestNbt.input(' +
                'new net.minecraft.nbt.CompoundTag()))')
        $text = $text.Replace(
            'ImmortalStorageExternalResourceKey.fromTag(' +
                'com.immortalstorage.immortalstorage.compat.CompatTestNbt.toTag(key))',
            'ImmortalStorageExternalResourceKey.fromTag(' +
                'com.immortalstorage.immortalstorage.compat.CompatTestNbt.input(' +
                'com.immortalstorage.immortalstorage.compat.CompatTestNbt.toTag(key)))')
        $text = $text.Replace(
            'AEKeyRendering.getOrThrow(ImmortalStorageExternalResourceKeyType.TYPE)',
            'AEKeyRendering.getOrThrow(new ImmortalStorageExternalResourceKey(' +
                'com.immortalstorage.core.resource.ExternalResourceChannels.FE))')

        # Target-only API and source-contract adaptations.  They retain the
        # same behavioral assertions while matching the official 26.1.2
        # names for deferred GUI rendering, capabilities and special models.
        $text = $text.Replace('OPEN_STORAGE.matches(event.getKeyCode(), event.getScanCode())',
            'OPEN_STORAGE.matches(new net.minecraft.client.input.KeyEvent(event.getKeyCode(), event.getScanCode(), 0))')
        $text = $text.Replace('graphics.renderFakeItem', 'graphics.fakeItem')
        $text = $text.Replace('"protected void renderSlotHighlight"', '"private void renderSlotHighlightCompat"')
        $text = $text.Replace('graphics.flush()', 'graphics.nextStratum()')
        $text = $text.Replace('"TerminalLayout.STORAGE_AMOUNT_Z"', '"graphics.text(this.font, label"')
        $text = $text.Replace('source.contains("extends BlockEntityWithoutLevelRenderer")',
            'source.contains("implements SpecialModelRenderer<ItemStack>")')
        $text = $text.Replace('source.contains("context == ItemDisplayContext.GUI")',
            'source.contains("SpecialModelGeometry.submitBlockBase")')
        $text = $text.Replace('registrations.contains("(be, side) -> be.getItemHandler(side)")',
            'registrations.contains("CompatTransfer.item(be.getItemHandler(side))")')
        $text = $text.Replace('registrations.contains("(be, side) -> be.getFluidHandler(side)")',
            'registrations.contains("CompatTransfer.fluid(be.getFluidHandler(side))")')
        $text = $text.Replace('methodBody(block, "protected void onRemove")',
            'methodBody(block, "protected void affectNeighborsAfterRemoval")')
        $text = $text.Replace('registration.contains("(be, side) -> be.getItemHandler()")',
            'registration.contains("CompatTransfer.item(be.getItemHandler())")')
        $text = $text.Replace('Capabilities.ItemHandler.BLOCK, TREASURE_BASIN.get()',
            'Capabilities.Item.BLOCK, TREASURE_BASIN.get()')
        $text = $text.Replace('methodBody(entity, "protected void saveAdditional(")',
            'methodBody(entity, "protected void saveAdditionalLegacy(")')
        $text = $text.Replace('methodBody(entity, "protected void loadAdditional(")',
            'methodBody(entity, "protected void loadAdditionalLegacy(")')
        $text = $text.Replace(
            'methodBody(entity, "public void removeComponentsFromTag")' + $newline +
                '                .contains("tag.remove(BUFFERS_TAG)")',
            'methodBody(entity, "public void removeComponentsFromTag")' + $newline +
                '                .contains("output.discard(BUFFERS_TAG)")')
        $text = $text.Replace('assertTrue(setup.contains("ModelEvent.RegisterAdditional"));',
            'assertTrue(setup.contains("RegisterSpecialModelRendererEvent"));')
        $text = $text.Replace('assertTrue(setup.contains("ModelIdentifier.standalone("));',
            'assertTrue(setup.contains("source_vein"));')
        $text = $text.Replace('assertTrue(blockItem.contains("getCustomRenderer()"));',
            'assertTrue(setup.contains("SourceVeinItemRenderer.Unbaked.MAP_CODEC"));')
        $text = $text.Replace('assertTrue(blockItem.contains("SourceVeinItemRenderer.INSTANCE"));',
            'assertTrue(renderer.contains("implements SpecialModelRenderer<ItemStack>"));')
        $text = $text.Replace('renderer.contains("extends BlockEntityWithoutLevelRenderer")',
            'renderer.contains("implements SpecialModelRenderer<ItemStack>")')
        $text = $text.Replace('renderer.contains("context == ItemDisplayContext.GUI")',
            'renderer.contains("SpecialModelGeometry.submitBlockBase")')
        $text = $text.Replace('renderer.contains("if (context == ItemDisplayContext.GUI) return;")',
            'renderer.contains("SpecialModelGeometry.submitNestedItem")')
        $text = $text.Replace('renderer.contains("renderModelLists(")',
            'renderer.contains("SpecialModelGeometry.submitBlockBase")')
        $text = $text.Replace('EnchantedBookItem.createForEnchantment',
            'EnchantmentHelper.createBook')
        $text = $text.Replace('VillagerProfession.LIBRARIAN', 'trade_set')
        $text = $text.Replace('versionRange=\"[19.2.17,19.3)\"',
            'versionRange=\"[26.1.10-beta,)\"')
        $text = $text.Replace('org.appliedenergistics:appliedenergistics2:19.2.17',
            'org.appliedenergistics:appliedenergistics2:26.1.10-beta:api')
        $text = $text.Replace('versionRange=\"[19.27.0.343,)\"',
            'versionRange=\"[29.21.0.68,)\"')
        $text = $text.Replace('mezz.jei:jei-1.21.1-common-api:19.27.0.343',
            'mezz.jei:jei-26.1.2-common-api:29.21.0.68')
        $text = $text.Replace('dev.emi:emi-neoforge:1.1.24+1.21.1:api',
            'dev.emi:emi-neoforge:26.1.2:api')
        $text = $text.Replace(
            'assertTrue(matrix.contains("dev.emi:emi-neoforge:26.1.2:api"));',
            'assertFalse(matrix.contains("dev.emi:emi-neoforge:26.1.2:api"));')
        $text = $text.Replace('versionRange=\"[0.3.0,0.4),[1.21.1-0.3.0,1.21.1-0.4)\"',
            'versionRange=\"[0,)\"')
        $text = $text.Replace('versionRange=\"[2.0.9,2.0.10)\"',
            'versionRange=\"[3.2.1,3.2.2)\"')
        if ($relativePath -match '[\\/]block[\\/]YuanLightContractTest\.java$') {
            $text = $text.Replace('getChunkNow(center.x, center.z)',
                'getChunkNow(center.x(), center.z())')
        }
        if ($relativePath -match '[\\/]ZeroPointZeroPointFourContractTest\.java$') {
            $text = $text.Replace(
                'private static final Path ROOT = locateProject();',
                'private static final Path ROOT = locateProject();' + $newline +
                    '    private static final Path TARGET_SOURCE_ROOT = ROOT.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source"))' + $newline +
                    '            .normalize();')
            $text = $text.Replace(
                'assertTrue(source("compat/emi/ImmortalStorageEmiPlugin.java").contains("VanillaEmiRecipeCategories.SMITHING"));',
                'assertFalse(Files.exists(TARGET_SOURCE_ROOT.resolve(Path.of("com", "immortalstorage", "immortalstorage", "compat", "emi", "ImmortalStorageEmiPlugin.java"))),' + $newline +
                    '                "EMI 26.1.2 is unavailable in the target matrix and remains an optional excluded adapter");')
            $text = [regex]::Replace($text,
                '(?s)assertTrue\(Files\.readString\(ROOT\.resolve\("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers\.json"\)\)\s*\.contains\("jade_guide_from_archaeology"\)\);',
                'assertTrue(Files.isRegularFile(ROOT.resolve("src/main/resources/data/immortalstorage/loot_modifiers/jade_guide_from_archaeology.json")));')
        }
        if ($relativePath -match '[\\/]enchantment[\\/]SpiritRepairResourceTest\.java$') {
            $text = $text.Replace(
                '.getAsJsonObject("minecraft:enchantments").getAsJsonObject("levels");',
                '.getAsJsonObject("minecraft:enchantments");')
        }
        if ($relativePath -match '[\\/]recipe[\\/]YuanRecipeSubstitutionTest\.java$') {
            $text = $text.Replace(
                '.getAsJsonArray("ingredients").get(0).getAsJsonObject());',
                '.getAsJsonArray("ingredients").get(0));')
            $text = $text.Replace(
                '.getAsJsonObject("key").getAsJsonObject("Y"));',
                '.getAsJsonObject("key").get("Y"));')
            $text = $text.Replace(
                '.getAsJsonObject("X").get("item").getAsString());',
                '.get("X").getAsString());')
            $text = $text.Replace(
                'private static void assertTagIngredient(String recipeName, JsonObject ingredient) {',
                'private static void assertTagIngredient(String recipeName, com.google.gson.JsonElement ingredient) {')
            $text = [regex]::Replace($text,
                'assertEquals\(YUAN_TAG, ingredient\.get\("tag"\)\.getAsString\(\), recipeName\);\s*' +
                    'assertFalse\(ingredient\.has\("item"\), recipeName \+ " must not hard-code true yuan"\);',
                'assertEquals("#" + YUAN_TAG, ingredient.getAsString(), recipeName);')
        }
        if ($relativePath -match '[\\/]worldgen[\\/]SpiritResourceContractTest\.java$') {
            $text = [regex]::Replace($text,
                'key\.getAsJsonObject\("([A-Z])"\)\.get\("item"\)\.getAsString\(\)',
                { param($match) 'key.get("' + $match.Groups[1].Value + '").getAsString()' })
            $text = $text.Replace(
                'assertEquals("c:raw_materials/spirit_iron",' + $newline +
                    '                blast.getAsJsonObject("ingredient").get("tag").getAsString());',
                'assertEquals("#c:raw_materials/spirit_iron",' + $newline +
                    '                blast.get("ingredient").getAsString());')
            $text = $text.Replace(
                'assertEquals("c:raw_materials/spirit_iron",',
                'assertEquals("#c:raw_materials/spirit_iron",')
            $text = $text.Replace(
                'blast.getAsJsonObject("ingredient").get("tag").getAsString()',
                'blast.get("ingredient").getAsString()')
            $text = $text.Replace(
                'immortalFurnace.getAsJsonObject("ingredient").get("item").getAsString());',
                'immortalFurnace.get("ingredient").getAsString());')
            $text = $text.Replace(
                'compact.getAsJsonObject("key").getAsJsonObject(String.valueOf(symbol)).get("item").getAsString()',
                'compact.getAsJsonObject("key").get(String.valueOf(symbol)).getAsString()')
            $text = $text.Replace(
                'unpack.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString()',
                'unpack.getAsJsonArray("ingredients").get(0).getAsString()')
        }
        if ($relativePath -match '[\\/]block[\\/]entity[\\/]XianqiaoInterfaceArchitectureContractTest\.java$') {
            $text = $text.Replace('tag.remove(BUFFERS_TAG)', 'output.discard(BUFFERS_TAG)')
        }
        if ($relativePath -match '[\\/]worldshard[\\/]TreasureBasinDecouplingTest\.java$') {
            $text = $text.Replace(
                'private static final Path PROJECT = locateProject();',
                'private static final Path PROJECT = locateProject();' + $newline +
                    '    private static final Path TARGET_SOURCE_ROOT = PROJECT.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage")).normalize();')
            $text = $text.Replace(
                'PROJECT.resolve("src/main/java/com/immortalstorage/immortalstorage/")',
                'TARGET_SOURCE_ROOT')
        }
    }

    # JEI 24.x replaced the old RecipeType/RecipeHolderType split with the
    # IRecipeType hierarchy.  Keep the 26.1.2 adapter on the target API rather
    # than compiling it against the 1.21.1 viewer jar by accident.
    if ($relativePath -match '[\\/]compat[\\/]jei[\\/]') {
        $text = $text.Replace('import mezz.jei.api.recipe.RecipeType;',
            'import mezz.jei.api.recipe.types.IRecipeType;')
        $text = $text.Replace('mezz.jei.api.recipe.RecipeType',
            'mezz.jei.api.recipe.types.IRecipeType')
        $text = $text.Replace('RecipeType.createRecipeHolderType(', 'IRecipeHolderType.create(')
        $text = [regex]::Replace($text, '(?<!I)RecipeType\.create\(', 'IRecipeType.create(')
        $text = [regex]::Replace($text, '(?<!I)RecipeType<', 'IRecipeType<')
        if ($text.Contains('IRecipeHolderType.create(') -and -not $text.Contains('import mezz.jei.api.recipe.types.IRecipeHolderType;')) {
            $text = $text.Replace('import mezz.jei.api.recipe.types.IRecipeType;',
                'import mezz.jei.api.recipe.types.IRecipeType;' + $newline +
                'import mezz.jei.api.recipe.types.IRecipeHolderType;')
        }
        $text = $text.Replace('RecipeIngredientRole.CATALYST', 'RecipeIngredientRole.CRAFTING_STATION')
    }

    # 26.1's partial sprite extraction path shifts these progress layers. Use
    # the complete PNG assets with pixel-space cropping and one shared painter.
    if ($relativePath -match '[\\/]client[\\/]screen[\\/]VanillaGuiPainter\.java$') {
        $text = $text.Replace('Identifier.withDefaultNamespace("container/furnace/burn_progress")',
            'Identifier.withDefaultNamespace("textures/gui/sprites/container/furnace/burn_progress.png")')
        $text = $text.Replace('"immortalstorage", "container/immortal_furnace/lit_progress")',
            '"immortalstorage", "textures/gui/sprites/container/immortal_furnace/lit_progress.png")')
        $text = $text.Replace('                    "immortalstorage", "textures/gui/sprites/container/immortal_furnace/lit_progress.png"))',
            '                    "immortalstorage", "textures/gui/sprites/container/immortal_furnace/lit_progress.png")')
        $text = [regex]::Replace($text,
            'com\.immortalstorage\.immortalstorage\.compat\.mc2612\.CompatGui\.blitSprite\(g, FURNACE_BURN_PROGRESS, 24, 16, 0, 0,\s*x \+ 82, y \+ laneY, progress, 16\);',
            'furnaceProgress(g, x + 82, y + laneY, progress);')
        $text = [regex]::Replace($text,
            'com\.immortalstorage\.immortalstorage\.compat\.mc2612\.CompatGui\.blitSprite\(g, IMMORTAL_FURNACE_LIT_PROGRESS, 14, 14, 0, 14 - height,\s*x, y \+ 14 - height, 14, height\);',
            'com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(g,' + $newline +
                '                IMMORTAL_FURNACE_LIT_PROGRESS, x, y + 14 - height, 14, height,' + $newline +
                '                0.0F, 14 - height, 14, 14);')
        $progressHelper =
            '    static void furnaceProgress(GuiGraphicsExtractor g, int x, int y, int progress) {' + $newline +
            '        int width = Mth.clamp(progress, 0, 24);' + $newline +
            '        if (width <= 0) return;' + $newline +
            '        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(g,' + $newline +
            '                FURNACE_BURN_PROGRESS, x, y, width, 16, 0.0F, 0.0F, 24, 16);' + $newline +
            '    }' + $newline + $newline
        $text = $text.Replace('    private static void furnaceArrow(', $progressHelper + '    private static void furnaceArrow(')
    }
    if ($relativePath -match '[\\/]client[\\/]screen[\\/](ImmortalFurnaceScreen|SimulatedSpiritFieldScreen|SimulatedReincarnationFurnaceScreen)\.java$') {
        $text = [regex]::Replace($text,
            '(?m)^\s*private static final Identifier BURN_PROGRESS =\s*\r?\n?\s*Identifier\.withDefaultNamespace\(\s*\r?\n?\s*"container/furnace/burn_progress"\);\s*\r?\n?', '')
        $text = [regex]::Replace($text,
            'com\.immortalstorage\.immortalstorage\.compat\.mc2612\.CompatGui\.blitSprite\(graphics, BURN_PROGRESS, 24, 16, 0, 0,\s*x \+ 95, laneY, width, 16\);',
            'VanillaGuiPainter.furnaceProgress(graphics, x + 95, laneY, width);')
        $text = [regex]::Replace($text,
            'com\.immortalstorage\.immortalstorage\.compat\.mc2612\.CompatGui\.blitSprite\(graphics, BURN_PROGRESS, 24, 16, 0, 0,\s*leftPos \+ 91, topPos \+ 44, progress, 16\);',
            'VanillaGuiPainter.furnaceProgress(graphics, leftPos + 91, topPos + 44, progress);')
    }
    if ($relativePath -match '[\\/]client[\\/]screen[\\/]VanillaArrowRenderingContractTest\.java$') {
        $text = $text.Replace('assertTrue(source.contains("textures/gui/container/furnace.png"))',
            'assertTrue(Files.readString(SCREEN_SOURCES.resolve("VanillaGuiPainter.java")).contains("textures/gui/container/furnace.png"))')
        $text = $text.Replace('assertTrue(source.contains("79.0F, 34.0F, 24, 16, 256, 256"))',
            'assertTrue(Files.readString(SCREEN_SOURCES.resolve("VanillaGuiPainter.java")).contains("79.0F, 34.0F, 24, 16, 256, 256"))')
        $text = $text.Replace('assertTrue(source.contains("container/furnace/burn_progress"))',
            'assertTrue(Files.readString(SCREEN_SOURCES.resolve("VanillaGuiPainter.java")).contains("textures/gui/sprites/container/furnace/burn_progress.png"))')
    }

    # A few targeted replacements above inject fully qualified factories. Keep
    # the final output on the official 26.1 name even for those injected calls.
    $text = $text.Replace('net.minecraft.resources.ResourceLocation', 'net.minecraft.resources.Identifier')
    $text = $text.Replace('ResourceLocation.fromNamespaceAndPath', 'Identifier.fromNamespaceAndPath')
    $text = $text.Replace('ResourceLocation.withDefaultNamespace', 'Identifier.withDefaultNamespace')
    $text = $text.Replace('ResourceLocation.tryParse', 'Identifier.tryParse')
    $text = $text.Replace('ResourceLocation.parse', 'Identifier.parse')
    $text = $text.Replace('ResourceLocation.read', 'Identifier.read')
    $text = $text.Replace('ResourceLocation', 'Identifier')

    # Canonical sources are LF-normalized in git. Environment.NewLine is used
    # above so exact Windows-source replacements remain readable, then the
    # generated tree is normalized once to avoid mixed CRLF/LF additions.
    return $text.Replace("`r`n", "`n")
}

$utf8 = [System.Text.UTF8Encoding]::new($false)

function Write-TextIfChanged([string] $Path, [string] $Contents) {
    if (Test-Path -LiteralPath $Path) {
        $existing = [System.IO.File]::ReadAllText($Path)
        if ($existing -ceq $Contents) {
            return
        }
    }
    [System.IO.File]::WriteAllText($Path, $Contents, $utf8)
}

function Copy-FileIfChanged([string] $Source, [string] $Destination) {
    if (Test-Path -LiteralPath $Destination) {
        $sourceInfo = Get-Item -LiteralPath $Source
        $destinationInfo = Get-Item -LiteralPath $Destination
        if ($sourceInfo.Length -eq $destinationInfo.Length) {
            $sourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
            $destinationHash = (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash
            if ($sourceHash -eq $destinationHash) {
                return
            }
        }
    }
    [System.IO.File]::Copy($Source, $Destination, $true)
}

$targetExcludedFragments = @(
    '/compat/arsnouveau/',
    '/compat/beyonddimensions/',
    '/compat/botania/',
    '/compat/buildinggadgets/',
    '/compat/emi/',
    '/compat/ifsouls/',
    '/compat/ironsspells/',
    '/compat/mekanism/',
    '/mixin/appliedbotanics/',
      '/mixin/arsnouveau/',
      '/mixin/buildinggadgets/',
    '/mixin/core/itemrendereroneqimuzzlemixin.java',
      '/client/render/sourceveinmodelbounds.java',
      '/recipe/yuansubstitutionshapedrecipe.java',
      '/recipe/yuansubstitutionshapelessrecipe.java',
      '/recipe/yuansubstitutionrecipesupport.java',
      '/recipe/immortalfurnacerecipe.java',
      '/recipe/modrecipes.java',
      # 结构模板矿石扫描器在 26.1.2 依赖已移除的 NbtIo.readCompressed/NbtAccounter，
      # 且 canonical 端尚未被任何调用方接入（孤立半成品）；设计文档第 4 节的被动
      # 区块观测才是结构矿物的完整解法，故目标版本不生成该文件。
      '/worldshard/worldshardstructureorescanner.java'
  )
$canonicalFiles = @(Get-ChildItem -LiteralPath $canonical -Filter '*.java' -Recurse -File |
    Where-Object {
        $normalized = $_.FullName.Replace('\', '/').ToLowerInvariant()
        -not ($targetExcludedFragments | Where-Object { $normalized.Contains($_) })
    })
$canonicalRelativePaths = @{}
foreach ($canonicalFile in $canonicalFiles) {
    $canonicalRelativePaths[$canonicalFile.FullName.Substring($canonical.Length).TrimStart('\', '/')] = $true
}
$overrideFiles = @()
if (Test-Path -LiteralPath $overrideRoot) {
    $overrideFiles = @(Get-ChildItem -LiteralPath $overrideRoot -Filter '*.java' -Recurse -File |
        Where-Object {
            $relativeOverride = $_.FullName.Substring($overrideRoot.Length).TrimStart('\', '/')
            $canonicalRelativePaths.ContainsKey($relativeOverride)
        })
}
$allowedGeneratedPaths = @{}
foreach ($canonicalFile in $canonicalFiles) {
    $relativeCanonical = $canonicalFile.FullName.Substring($canonical.Length).TrimStart('\', '/')
    if (-not (Test-Path -LiteralPath (Join-Path $overrideRoot $relativeCanonical))) {
        $allowedGeneratedPaths[$relativeCanonical] = $true
    }
}
if (Test-Path -LiteralPath $target) {
    foreach ($staleFile in @(Get-ChildItem -LiteralPath $target -Filter '*.java' -Recurse -File)) {
        $relativeStale = $staleFile.FullName.Substring($target.Length).TrimStart('\', '/')
        if (-not $allowedGeneratedPaths.ContainsKey($relativeStale)) {
            Remove-Item -LiteralPath $staleFile.FullName -Force
        }
    }
}
foreach ($sourceFile in $canonicalFiles) {
    $relative = $sourceFile.FullName.Substring($canonical.Length).TrimStart('\', '/')
    $override = Join-Path $overrideRoot $relative
    if (Test-Path -LiteralPath $override) {
        continue
    }
    $destination = Join-Path $target $relative
    $parent = Split-Path -Parent $destination
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    $contents = [System.IO.File]::ReadAllText($sourceFile.FullName)
    $contents = Transform-2612 $contents $relative
    Write-TextIfChanged $destination $contents
}

# Minecraft 26.1 resolves an item's client model through
# assets/<namespace>/items/<path>.json.  The canonical 1.21.1 lane stores the
# equivalent model under models/item and uses BEWLR/mixin hooks for the
# dynamic previews.  Keep the target resource view generated beside the
# migrated Java sources so a fresh compatibility generation cannot silently
# lose the inventory renderer entry points.
$canonicalResourceRoot = Join-Path (Split-Path -Parent $canonical) 'resources'
$targetResourceRoot = Join-Path (Split-Path -Parent $target) 'resources'
$canonicalRecipeRoot = Join-Path $canonicalResourceRoot 'data/immortalstorage/recipe'
$targetRecipeRoot = Join-Path $targetResourceRoot 'data/immortalstorage/recipe'
$canonicalLootModifierRoot = Join-Path $canonicalResourceRoot 'data/immortalstorage/loot_modifiers'
$targetLootModifierRoot = Join-Path $targetResourceRoot 'data/immortalstorage/loot_modifiers'
foreach ($resourcePair in @(
        @($canonicalRecipeRoot, $targetRecipeRoot, 'recipe'),
        @($canonicalLootModifierRoot, $targetLootModifierRoot, 'loot modifier'))) {
    $canonicalDataRoot = $resourcePair[0]
    $targetDataRoot = $resourcePair[1]
    $resourceLabel = $resourcePair[2]
    if (-not (Test-Path -LiteralPath $canonicalDataRoot)) {
        throw "Canonical 26.1.2 migration source root is missing: $canonicalDataRoot"
    }
    New-Item -ItemType Directory -Path $targetDataRoot -Force | Out-Null
    $canonicalNames = @{}
    foreach ($canonicalDataFile in @(Get-ChildItem -LiteralPath $canonicalDataRoot -Filter '*.json' -File)) {
        $canonicalNames[$canonicalDataFile.Name] = $true
    }
    foreach ($staleDataFile in @(Get-ChildItem -LiteralPath $targetDataRoot -Filter '*.json' -File)) {
        if (-not $canonicalNames.ContainsKey($staleDataFile.Name)) {
            Remove-Item -LiteralPath $staleDataFile.FullName -Force
        }
    }
    foreach ($canonicalDataFile in @(Get-ChildItem -LiteralPath $canonicalDataRoot -Filter '*.json' -File)) {
        $targetDataFile = Join-Path $targetDataRoot $canonicalDataFile.Name
        $canonicalJson = [System.IO.File]::ReadAllText($canonicalDataFile.FullName) | ConvertFrom-Json -Depth 100
        if ($resourceLabel -eq 'recipe') {
            $targetJson = Convert-2612RecipeNode $canonicalJson
            if ($canonicalDataFile.BaseName -in @('spirit_staff', 'spirit_sword')) {
                if ($null -eq $targetJson['result'] -or $null -eq $targetJson['result']['components']) {
                    throw "Cannot migrate enchantment component in recipe: $($canonicalDataFile.FullName)"
                }
                $targetJson['result']['components']['minecraft:enchantments'] =
                    [ordered]@{ 'immortalstorage:spirit_repair' = 1 }
            }
        } else {
            $targetJson = Convert-2612LootModifierNode $canonicalJson
        }
        $targetJsonText = $targetJson | ConvertTo-Json -Depth 100
        Write-TextIfChanged $targetDataFile ($targetJsonText + $newline)
    }
}
$targetLegacyLootIndex = Join-Path $targetResourceRoot 'data/neoforge/loot_modifiers/global_loot_modifiers.json'
if (Test-Path -LiteralPath $targetLegacyLootIndex) {
    Remove-Item -LiteralPath $targetLegacyLootIndex -Force
}
Write-Output "Generated 26.1.2 $($canonicalRecipeRoot | Split-Path -Leaf) and loot modifier resource overlays"
$canonicalItemModelRoot = Join-Path $canonicalResourceRoot 'assets/immortalstorage/models/item'
$targetItemDefinitionRoot = Join-Path $targetResourceRoot 'assets/immortalstorage/items'
if (-not (Test-Path -LiteralPath $canonicalItemModelRoot)) {
    throw "Canonical item model resource root is missing: $canonicalItemModelRoot"
}
New-Item -ItemType Directory -Path $targetItemDefinitionRoot -Force | Out-Null

$canonicalModBlocks = Join-Path $canonical 'com/immortalstorage/immortalstorage/block/ModBlocks.java'
$modBlocksText = [System.IO.File]::ReadAllText($canonicalModBlocks)
$sourceVeinIds = @([regex]::Matches($modBlocksText,
        'reg\("([^"]+)",\s*\(\)\s*->\s*new SourceVeinBlock') |
    ForEach-Object { $_.Groups[1].Value })
$sourceVeinManagerIds = @([regex]::Matches($modBlocksText,
        'reg\("([^"]+)",\s*\n?\s*\(\)\s*->\s*new SourceVeinManagerBlock') |
    ForEach-Object { $_.Groups[1].Value })
$dynamicPreviewIds = @('stabilized_miniature_immortal_ruin', 'xianqiao_manager')

foreach ($modelFile in @(Get-ChildItem -LiteralPath $canonicalItemModelRoot -Filter '*.json' -File |
        Where-Object { -not $_.BaseName.EndsWith('_base') })) {
    $itemId = $modelFile.BaseName
    $definitionPath = Join-Path $targetItemDefinitionRoot ($itemId + '.json')
    if ($itemId -eq 'spirit_staff' -and (Test-Path -LiteralPath $definitionPath)) {
        continue
    }

    if ($sourceVeinIds -contains $itemId) {
        $baseModel = if ($itemId -eq 'custom_source_vein') {
            'immortalstorage:item/custom_source_vein'
        } else {
            'immortalstorage:item/source_vein'
        }
        $definition = [ordered]@{
            model = [ordered]@{
                type = 'minecraft:special'
                base = $baseModel
                model = [ordered]@{ type = 'immortalstorage:source_vein' }
            }
        }
    } elseif ($sourceVeinManagerIds -contains $itemId) {
        $definition = [ordered]@{
            model = [ordered]@{
                type = 'minecraft:special'
                base = 'immortalstorage:item/source_vein_manager'
                model = [ordered]@{ type = 'immortalstorage:source_vein_manager' }
            }
        }
    } elseif ($dynamicPreviewIds -contains $itemId) {
        $definition = [ordered]@{
            model = [ordered]@{
                type = 'minecraft:special'
                base = "immortalstorage:item/$itemId"
                model = [ordered]@{ type = 'immortalstorage:dynamic_preview' }
            }
        }
    } else {
        $definition = [ordered]@{
            model = [ordered]@{
                type = 'minecraft:model'
                model = "immortalstorage:item/$itemId"
            }
        }
    }
    $json = $definition | ConvertTo-Json -Depth 8
    Write-TextIfChanged $definitionPath ($json + $newline)
}

Write-Output "Generated target 26.1.2 client item definitions at $targetItemDefinitionRoot"

$generatedFiles = Get-ChildItem -LiteralPath $target -Filter '*.java' -Recurse -File
if ($generatedFiles.Count -ne ($canonicalFiles.Count - $overrideFiles.Count)) {
    throw 'Compatibility source generation lost or added a canonical Java source file.'
}
$generatedText = ($generatedFiles | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
foreach ($forbidden in @('ItemInteractionResult', 'INBTSerializable', 'ResourceLocation', 'net.minecraft.advancements.critereon')) {
    if ($generatedText.Contains($forbidden)) {
        throw "26.1.2 generation assertion failed; forbidden canonical API remains: $forbidden"
    }
}
$keybindGeneratedFile = $generatedFiles | Where-Object {
    $_.FullName -match '[\\/]client[\\/]keybind[\\/]ImmortalStorageKeybinds\.java$'
} | Select-Object -First 1
if ($null -eq $keybindGeneratedFile) {
    throw '26.1.2 generation assertion failed; migrated keybind source is missing.'
}
$keybindGeneratedText = [System.IO.File]::ReadAllText($keybindGeneratedFile.FullName)
$keybindCategoryRegistrationCount = ([regex]::Matches(
        $keybindGeneratedText, 'KeyMapping\.Category\.register\(')).Count
if ($keybindCategoryRegistrationCount -ne 1) {
    throw "26.1.2 generation assertion failed; expected one key category registration, found $keybindCategoryRegistrationCount."
}
if (-not $keybindGeneratedText.Contains('IMMORTALSTORAGE_CATEGORY')) {
    throw '26.1.2 generation assertion failed; key mappings do not share the generated category field.'
}
if ([regex]::IsMatch($generatedText, '(?m)\bclass\s+\w+(?:\s*<[^{}]*>)?\s+extends\s+BlockEntity\b')) {
    throw '26.1.2 generation assertion failed; a block entity class still extends the canonical BlockEntity type.'
}
if ($generatedText.Contains('saveAdditional(CompoundTag') -or $generatedText.Contains('loadAdditional(CompoundTag')) {
    throw '26.1.2 generation assertion failed; a legacy block-entity hook was not renamed.'
}
if ($generatedText.Contains('`')) {
    throw '26.1.2 generation assertion failed; a PowerShell escape marker leaked into generated Java.'
}

$targetResourceRoot = Join-Path (Split-Path -Parent $target) 'resources'
$targetDimensionResource = Join-Path $targetResourceRoot 'data/immortalstorage/dimension_type/xianqiao_realm.json'
$targetBiomeResource = Join-Path $targetResourceRoot 'data/immortalstorage/worldgen/biome/xianqiao_realm.json'
foreach ($requiredTargetResource in @($targetDimensionResource, $targetBiomeResource)) {
    if (-not (Test-Path -LiteralPath $requiredTargetResource -PathType Leaf)) {
        throw "26.1.2 generation assertion failed; target worldgen resource is missing: $requiredTargetResource"
    }
}
$targetForbiddenTagPaths = @(
    (Join-Path $targetResourceRoot 'data/minecraft/tags/block/iron_ores.json'),
    (Join-Path $targetResourceRoot 'data/minecraft/tags/block/diamond_ores.json'),
    (Join-Path $targetResourceRoot 'data/minecraft/tags/item/iron_ores.json'),
    (Join-Path $targetResourceRoot 'data/minecraft/tags/item/diamond_ores.json'))
foreach ($forbiddenTargetTagPath in $targetForbiddenTagPaths) {
    if (Test-Path -LiteralPath $forbiddenTargetTagPath) {
        throw "26.1.2 generation assertion failed; mineral tag overlay is outside the requested worldgen-only migration: $forbiddenTargetTagPath"
    }
}
$targetRecipeRoot = Join-Path $targetResourceRoot 'data/immortalstorage/recipe'
$targetLootModifierRoot = Join-Path $targetResourceRoot 'data/immortalstorage/loot_modifiers'
if (@(Get-ChildItem -LiteralPath $targetRecipeRoot -Filter '*.json' -File).Count -ne
    @(Get-ChildItem -LiteralPath $canonicalRecipeRoot -Filter '*.json' -File).Count) {
    throw '26.1.2 generation assertion failed; target recipe overlay is incomplete.'
}
if (@(Get-ChildItem -LiteralPath $targetLootModifierRoot -Filter '*.json' -File).Count -ne
    @(Get-ChildItem -LiteralPath $canonicalLootModifierRoot -Filter '*.json' -File).Count) {
    throw '26.1.2 generation assertion failed; target loot-modifier overlay is incomplete.'
}
if (Test-Path -LiteralPath $targetLegacyLootIndex) {
    throw '26.1.2 generation assertion failed; legacy global loot-modifier index is still present.'
}
$targetDimensionText = [System.IO.File]::ReadAllText($targetDimensionResource)
foreach ($requiredDimensionToken in @('"attributes"', '"default_clock"', '"has_ender_dragon_fight"', '"timelines"')) {
    if (-not $targetDimensionText.Contains($requiredDimensionToken)) {
        throw "26.1.2 generation assertion failed; target dimension type still lacks $requiredDimensionToken"
    }
}
$targetBiomeText = [System.IO.File]::ReadAllText($targetBiomeResource)
if ($targetBiomeText.Contains('"carvers": {}') -or -not $targetBiomeText.Contains('"carvers": []')) {
    throw '26.1.2 generation assertion failed; target biome still uses the legacy carvers object.'
}
if (-not [string]::IsNullOrWhiteSpace($TargetAuditSourceRoot)) {
    $auditRoot = [System.IO.Path]::GetFullPath($TargetAuditSourceRoot)
    if ([string]::IsNullOrWhiteSpace($auditRoot) -or $auditRoot -eq [System.IO.Path]::GetPathRoot($auditRoot)) {
        throw "Refusing to generate audit sources into an empty or filesystem-root target: $auditRoot"
    }
    New-Item -ItemType Directory -Path $auditRoot -Force | Out-Null
    $allowedAuditPaths = @{}
    foreach ($sourceFile in $generatedFiles) {
        $relativeGenerated = $sourceFile.FullName.Substring($target.Length).TrimStart('\', '/')
        $allowedAuditPaths[$relativeGenerated] = $true
    }
    foreach ($overrideFile in @(Get-ChildItem -LiteralPath $overrideRoot -Filter '*.java' -Recurse -File)) {
        $relativeOverride = $overrideFile.FullName.Substring($overrideRoot.Length).TrimStart('\', '/')
        $allowedAuditPaths[$relativeOverride] = $true
    }
    foreach ($staleAudit in @(Get-ChildItem -LiteralPath $auditRoot -Filter '*.java' -Recurse -File)) {
        $relativeStaleAudit = $staleAudit.FullName.Substring($auditRoot.Length).TrimStart('\', '/')
        if (-not $allowedAuditPaths.ContainsKey($relativeStaleAudit)) {
            Remove-Item -LiteralPath $staleAudit.FullName -Force
        }
    }
    foreach ($sourceFile in $generatedFiles) {
        $relativeGenerated = $sourceFile.FullName.Substring($target.Length).TrimStart('\', '/')
        $destinationAudit = Join-Path $auditRoot $relativeGenerated
        New-Item -ItemType Directory -Path (Split-Path -Parent $destinationAudit) -Force | Out-Null
        Copy-FileIfChanged $sourceFile.FullName $destinationAudit
    }
    # Target overrides are the effective source for files deliberately kept
    # out of generated-java (notably the 26.1 rendering bridges).  Overlay
    # them after generated files so source-contract tests see the same merged
    # view that Gradle compiles.
    foreach ($overrideFile in @(Get-ChildItem -LiteralPath $overrideRoot -Filter '*.java' -Recurse -File)) {
        $relativeOverride = $overrideFile.FullName.Substring($overrideRoot.Length).TrimStart('\', '/')
        $destinationAudit = Join-Path $auditRoot $relativeOverride
        New-Item -ItemType Directory -Path (Split-Path -Parent $destinationAudit) -Force | Out-Null
        Copy-FileIfChanged $overrideFile.FullName $destinationAudit
    }
    Write-Output "Generated merged 26.1.2 audit source view at $auditRoot"
}
Write-Output "Generated $($generatedFiles.Count) 26.1.2 compatibility sources at $target"

if ([string]::IsNullOrWhiteSpace($CanonicalTestRoot) -xor [string]::IsNullOrWhiteSpace($TargetTestRoot)) {
    throw 'CanonicalTestRoot and TargetTestRoot must be supplied together.'
}

if (-not [string]::IsNullOrWhiteSpace($CanonicalTestRoot)) {
    $canonicalTests = (Resolve-Path -LiteralPath $CanonicalTestRoot).Path
    $targetTests = [System.IO.Path]::GetFullPath($TargetTestRoot)
    if ([string]::IsNullOrWhiteSpace($targetTests) -or $targetTests -eq [System.IO.Path]::GetPathRoot($targetTests)) {
        throw "Refusing to generate tests into an empty or filesystem-root target: $targetTests"
    }
    New-Item -ItemType Directory -Path $targetTests -Force | Out-Null

    # The audited 26.1.2 matrix contains AE2, Refined Storage and JEI.  The
    # remaining optional integrations have no target artifact in the matrix;
    # their direct adapter tests stay on the 1.21.1 lane while their
    # top-level optional-boundary tests are still migrated below.
    $targetTestExcludedFragments = @(
        '/compat/arsnouveau/',
        '/compat/beyonddimensions/',
        '/compat/botania/',
        '/compat/emi/',
        '/compat/ifsouls/',
        '/compat/mekanism/',
        '/compat/arsnouveauoptionalboundarytest.java',
        '/compat/beyonddimensionsoptionalboundarytest.java',
        '/compat/botaniaoptionalboundarytest.java',
        '/compat/industrialforegoingsoulsoptionalboundarytest.java',
        '/compat/mekanismoptionalboundarytest.java',
        '/compat/refinedstorage/rsinstalledaddonreflectioncontracttest.java',
        '/client/screen/xianqiaointerfacemodallayoutcontracttest.java',
        '/compat/xianqiaointerfaceviewerghostconfigurationtest.java',
        '/block/entity/xianqiaointerfacepassivepipecontracttest.java'
    )
    $canonicalTestFiles = @(Get-ChildItem -LiteralPath $canonicalTests -Filter '*.java' -Recurse -File |
        Where-Object {
            $normalized = $_.FullName.Replace('\', '/').ToLowerInvariant()
            -not ($targetTestExcludedFragments | Where-Object { $normalized.Contains($_) })
        })
    $allowedTestPaths = @{}
    foreach ($testFile in $canonicalTestFiles) {
        $relativeTest = $testFile.FullName.Substring($canonicalTests.Length).TrimStart('\', '/')
        $allowedTestPaths[$relativeTest] = $true
    }
    foreach ($staleTest in @(Get-ChildItem -LiteralPath $targetTests -Filter '*.java' -Recurse -File)) {
        $relativeStaleTest = $staleTest.FullName.Substring($targetTests.Length).TrimStart('\', '/')
        if (-not $allowedTestPaths.ContainsKey($relativeStaleTest)) {
            Remove-Item -LiteralPath $staleTest.FullName -Force
        }
    }
    foreach ($testFile in $canonicalTestFiles) {
        $relativeTest = $testFile.FullName.Substring($canonicalTests.Length).TrimStart('\', '/')
        $destinationTest = Join-Path $targetTests $relativeTest
        $parentTest = Split-Path -Parent $destinationTest
        New-Item -ItemType Directory -Path $parentTest -Force | Out-Null
        $testContents = [System.IO.File]::ReadAllText($testFile.FullName)
        $testContents = Transform-2612 $testContents ('src/test/java/' + $relativeTest)
        Write-TextIfChanged $destinationTest $testContents
    }

    $generatedTests = @(Get-ChildItem -LiteralPath $targetTests -Filter '*.java' -Recurse -File)
    if ($generatedTests.Count -ne $canonicalTestFiles.Count) {
        throw '26.1.2 test generation lost or added a canonical Java test source file.'
    }
    $generatedTestText = ($generatedTests | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
    foreach ($forbiddenTestApi in @('ItemInteractionResult', 'net.minecraft.advancements.critereon', 'ResourceLocation')) {
        if ($generatedTestText.Contains($forbiddenTestApi)) {
            throw "26.1.2 test generation assertion failed; forbidden canonical API remains: $forbiddenTestApi"
        }
    }
    Write-Output "Generated $($generatedTests.Count) 26.1.2 compatibility tests at $targetTests"
}
