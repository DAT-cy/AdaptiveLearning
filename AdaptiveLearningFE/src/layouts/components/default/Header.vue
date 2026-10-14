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
    title: 'Hộp thoại cảnh báo',
    href: '/docs/primitives/alert-dialog',
    description:
      'Hộp thoại hiển thị nội dung quan trọng và yêu cầu người dùng phản hồi.',
  },
  {
    title: 'Thẻ xem trước',
    href: '/docs/primitives/hover-card',
    description:
      'Hiển thị bản xem trước nội dung được liên kết khi người dùng di chuột.',
  },
  {
    title: 'Tiến độ',
    href: '/docs/primitives/progress',
    description:
      'Hiển thị mức độ hoàn thành của một nhiệm vụ, thường bằng thanh tiến độ.',
  },
  {
    title: 'Vùng cuộn',
    href: '/docs/primitives/scroll-area',
    description: 'Phân tách nội dung bằng bố cục hoặc ngữ nghĩa.',
  },
  {
    title: 'Thẻ nội dung',
    href: '/docs/primitives/tabs',
    description:
      'Tập hợp các vùng nội dung xếp lớp, mỗi lần chỉ hiển thị một vùng.',
  },
  {
    title: 'Chú giải',
    href: '/docs/primitives/tooltip',
    description:
      'Thông tin bổ sung xuất hiện khi phần tử được focus hoặc khi di chuột lên.',
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
            <NavigationMenuTrigger>Bắt đầu</NavigationMenuTrigger>
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
                        Các thành phần giao diện được xây dựng bằng Radix UI và Tailwind CSS.
                      </p>
                    </a>
                  </NavigationMenuLink>
                </li>
                <a href="/docs" title="Giới thiệu">
                  Các thành phần giao diện có thể tái sử dụng, xây dựng bằng Radix UI và Tailwind CSS.
                </a>
                <a href="/docs/installation" title="Cài đặt">
                  Cách cài đặt các thư viện và tổ chức cấu trúc ứng dụng.
                </a>
                <a href="/docs/primitives/typography" title="Kiểu chữ">
                  Kiểu hiển thị cho tiêu đề, đoạn văn, danh sách và các thành phần khác.
                </a>
              </ul>
            </NavigationMenuContent>
          </NavigationMenuItem>
          <NavigationMenuItem>
            <NavigationMenuTrigger>Thành phần</NavigationMenuTrigger>
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
              Tài liệu
            </NavigationMenuLink>
          </NavigationMenuItem>
        </NavigationMenuList>
      </NavigationMenu>
      <div class="space-x-4">
        <Button variant="outline">Đăng nhập</Button>
        <Button>Đăng ký</Button>
      </div>
    </div>
  </header>
</template>
