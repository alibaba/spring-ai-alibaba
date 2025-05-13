import Jump from './Jump';

const Button = (props) =>{
    const {
        href,
        type = "normal",
        size = "medium",
        visibility = true,
        target = "_self",
        iconClass = "",
        children,
    } = props;

    return (
        <a
            href={href}
            target={target}
            class={`
                group
                btn transition-shadow ease-in-out duration-200
                flex items-center justify-center text-sm rounded-3xl no-underline 
                button-${type || 'normal'} 
                bg-${type || 'normal'} 
                link-button-bg-${type}
                ${size === 'small' ? 'xp-small h-small' : ''} 
                ${size === 'medium' ? 'xp-medium h-medium' : ''} 
                ${size === 'large' ? 'xp-large h-large' : ''} 
                ${props?.class || ''}
            `}
        >
        {children}
        {
            visibility && (
            <span class="icon group-hover:ml-2 group-hover:opacity-100 group-hover:w-[8px] group-hover:translate-x-[3px] opacity-0 w-0 duration-200 ease-in-out">
                <Jump class={iconClass}/>
            </span>
            )
        }
        </a>
    );
};

export default Button;