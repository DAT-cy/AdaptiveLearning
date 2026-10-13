<script setup>
import Button from '@/components/ui/button/Button.vue';
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
  navigationMenuTriggerStyle,
} from '@/components/ui/navigation-menu'

const components = [
  {
    title: 'Alert Dialog',
    href: '/docs/primitives/alert-dialog',
    description:
      'A modal dialog that interrupts the user with important content and expects a response.',
  },
  {
    title: 'Hover Card',
    href: '/docs/primitives/hover-card',
    description:
      'For sighted users to preview content available behind a link.',
  },
  {
    title: 'Progress',
    href: '/docs/primitives/progress',
    description:
      'Displays an indicator showing the completion progress of a task, typically displayed as a progress bar.',
  },
  {
    title: 'Scroll-area',
    href: '/docs/primitives/scroll-area',
    description: 'Visually or semantically separates content.',
  },
  {
    title: 'Tabs',
    href: '/docs/primitives/tabs',
    description:
      'A set of layered sections of content—known as tab panels—that are displayed one at a time.',
  },
  {
    title: 'Tooltip',
    href: '/docs/primitives/tooltip',
    description:
      'A popup that displays information related to an element when the element receives keyboard focus or the mouse hovers over it.',
  },
]
</script>

<template>
  <header class="py-4 bg-muted-foreground">
    <div class="container flex flex-wrap items-center justify-between mx-auto ">
      <div class="w-12 h-12 overflow-hidden">
        <img class="object-cover w-full h-full" src="@/assets/images/Login.png" alt="">
      </div>
      <NavigationMenu>
        <NavigationMenuList>
          <NavigationMenuItem>
            <NavigationMenuTrigger>Getting started</NavigationMenuTrigger>
            <NavigationMenuContent>
              <ul class="grid gap-3 p-6 md:w-[400px] lg:w-[500px] lg:grid-cols-[minmax(0,.75fr)_minmax(0,1fr)]">
                <li class="row-span-3">
                  <NavigationMenuLink as-child>
                    <a class="flex flex-col justify-end w-full h-full p-6 no-underline rounded-md outline-none select-none bg-gradient-to-b from-muted/50 to-muted focus:shadow-md"
                      href="/">
                      <img src="https://www.radix-vue.com/logo.svg" class="w-6 h-6">
                      <div class="mt-4 mb-2 text-lg font-medium">
                        shadcn/ui
                      </div>
                      <p class="text-sm leading-tight text-muted-foreground">
                        Beautifully designed components built with Radix UI and
                        Tailwind CSS.
                      </p>
                    </a>
                  </NavigationMenuLink>
                </li>
                <a href="/docs" title="Introduction">
                  Re-usable components built using Radix UI and Tailwind CSS.
                </a>
                <a href="/docs/installation" title="Installation">
                  How to install dependencies and structure your app.
                </a>
                <a href="/docs/primitives/typography" title="Typography">
                  Styles for headings, paragraphs, lists...etc
                </a>
              </ul>
            </NavigationMenuContent>
          </NavigationMenuItem>
          <NavigationMenuItem>
            <NavigationMenuTrigger>Components</NavigationMenuTrigger>
            <NavigationMenuContent>
              <ul class="grid w-[400px] gap-3 p-4 md:w-[500px] md:grid-cols-2 lg:w-[600px] ">
                <a v-for="component in components" :key="component.title" :title="component.title"
                  :href="component.href">
                  {{ component.description }}
                </a>
              </ul>
            </NavigationMenuContent>
          </NavigationMenuItem>
          <NavigationMenuItem>
            <NavigationMenuLink href="/docs" :class="navigationMenuTriggerStyle()">
              Documentation
            </NavigationMenuLink>
          </NavigationMenuItem>
        </NavigationMenuList>
      </NavigationMenu>
      <div class="space-x-4">
        <Button variant="outline">Login</Button>
        <Button>Signup</Button>
      </div>
    </div>
  </header>
</template>